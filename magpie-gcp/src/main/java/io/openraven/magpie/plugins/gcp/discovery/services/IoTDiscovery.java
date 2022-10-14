/*
 * Copyright 2021 Open Raven Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openraven.magpie.plugins.gcp.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.CredentialsProvider;
import com.google.appengine.repackaged.com.google.common.base.Pair;
import com.google.cloud.iot.v1.*;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gcp.iot.IotDeviceRegistry;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IoTDiscovery implements GCPDiscovery {
  private static final String SERVICE = "iot";

  private static final List<String> AVAILABLE_LOCATIONS = List.of("asia-east1", "europe-west1", "us-central1");

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger, Optional<CredentialsProvider> maybeCredentialsProvider) {
    final String RESOURCE_TYPE = IotDeviceRegistry.RESOURCE_TYPE;
    var builder = DeviceManagerSettings.newBuilder();
    maybeCredentialsProvider.ifPresent(builder::setCredentialsProvider);

    try (DeviceManagerClient deviceManagerClient = DeviceManagerClient.create(builder.build())) {
      AVAILABLE_LOCATIONS.forEach(location -> {
        String parent = LocationName.of(projectId, location).toString();

        deviceManagerClient.listDeviceRegistries(parent).iterateAll()
          .forEach(deviceRegistry -> {
            var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, deviceRegistry.getName())
              .withProjectId(projectId)
              .withResourceType(RESOURCE_TYPE)
              .withRegion(location)
              .withConfiguration(GCPUtils.asJsonNode(deviceRegistry))
              .build();

            discoverDevices(deviceManagerClient, deviceRegistry, data);

            emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":deviceRegistry"), data.toJsonNode()));
          });
      });
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverDevices(DeviceManagerClient deviceManagerClient,
                               DeviceRegistry deviceRegistry,
                               MagpieGcpResource data) {
    final String fieldName = "devices";

    ArrayList<Device.Builder> list = new ArrayList<>();
    deviceManagerClient.listDevices(deviceRegistry.getName()).iterateAll()
      .forEach(device -> list.add(device.toBuilder()));

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, list));
  }
}
