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
import com.google.appengine.repackaged.com.google.common.base.Pair;
import com.google.cloud.compute.v1.*;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ComputeEngineDiscovery implements GCPDiscovery {
  private static final String SERVICE = "computeEngine";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger) {
    discoverZones(mapper, projectId, session, emitter);
  }

  private void discoverZones(ObjectMapper mapper, String projectId, Session session, Emitter emitter) {
    final String RESOURCE_TYPE = "GCP::ComputeEngine::Zone";

    try (var client = ZoneClient.create()) {
      for (var zone : client.listZones(projectId).iterateAll()) {
        String assetId = String.format("%s::%s", RESOURCE_TYPE, zone.getName());
        var data = new MagpieResource.MagpieResourceBuilder(mapper, assetId)
          .withProjectId(projectId)
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(GCPUtils.asJsonNode(zone))
          .build();

        discoverInstances(zone, projectId, data);
        discoverDisks(zone, projectId, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":zone"), data.toJsonNode()));
      }
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverInstances(Zone zone, String projectId, MagpieResource data) {
    final String fieldName = "instances";

    try (var instancesClient = InstanceClient.create()) {
      ArrayList<Instance.Builder> list = new ArrayList<>();
      instancesClient.listInstances(ProjectZoneName.of(projectId, zone.getName())).iterateAll()
        .forEach(task -> list.add(task.toBuilder()));

      GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, list));
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException("GCP::ComputeEngine::Zone::Instance", e);
    }
  }

  private void discoverDisks(Zone zone, String projectId, MagpieResource data) {
    final String fieldName = "disks";

    try (var instancesClient = DiskClient.create()) {
      ArrayList<Disk.Builder> list = new ArrayList<>();
      instancesClient.listDisks(ProjectZoneName.of(projectId, zone.getName())).iterateAll()
        .forEach(task -> list.add(task.toBuilder()));

      GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, list));
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException("GCP::ComputeEngine::Zone::Disk", e);
    }
  }
}
