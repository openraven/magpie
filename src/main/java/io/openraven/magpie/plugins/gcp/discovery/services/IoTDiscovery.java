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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.repackaged.com.google.gson.GsonBuilder;
import com.google.cloud.iot.v1.DeviceManagerClient;
import com.google.cloud.iot.v1.DeviceRegistry;
import com.google.cloud.iot.v1.LocationName;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPResource;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

public class IoTDiscovery implements GCPDiscovery {
  private static final String SERVICE = "iot";

  private static final List<String> AVAILABLE_LOCATIONS = List.of("asia-east1", "europe-west1", "us-central1");

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(String projectId, ObjectMapper mapper, Session session, Emitter emitter, Logger logger) {
    final String RESOURCE_TYPE = "GCP::IoT::deviceRegistry";

    try (DeviceManagerClient deviceManagerClient = DeviceManagerClient.create()) {
      AVAILABLE_LOCATIONS.forEach(location -> {
        String parent = LocationName.of(projectId, location).toString();
        for (DeviceRegistry deviceRegistry : deviceManagerClient.listDeviceRegistries(parent).iterateAll()) {
          var data = new GCPResource(deviceRegistry.getName(), projectId, RESOURCE_TYPE, mapper);

          String secretJsonString = new GsonBuilder().setPrettyPrinting().create().toJson(deviceRegistry);
          try {
            data.configuration = mapper.readValue(secretJsonString, JsonNode.class);
          } catch (JsonProcessingException e) {
            logger.error("Unexpected JsonProcessingException this shouldn't happen at all");
          }

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":deviceRegistry"), data.toJsonNode(mapper)));
        }
      });
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }
}
