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
import com.google.cloud.compute.v1.*;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

import static io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider.create;

public class ComputeEngineDiscovery implements GCPDiscovery {
  private static final String SERVICE = "computeEngine";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger) {
    discoverInstances(mapper, projectId, session, emitter);
    discoverDisks(mapper, projectId, session, emitter);
  }

  private void discoverInstances(ObjectMapper mapper, String projectId, Session session, Emitter emitter) {
    final String RESOURCE_TYPE = "GCP::ComputeEngine::Instance";

    try (var instancesClient = InstanceClient.create();
         var zoneClient = ZoneClient.create()) {
      // On2 - we are listing all instances in all zones
      zoneClient.listZones(projectId).iterateAll().forEach(zone -> {

        instancesClient.listInstances(ProjectZoneName.of(projectId, zone.getName())).iterateAll()
          .forEach(instance -> {
            String assetId = String.format("%s::%s", instance.getName(), instance.getId());
            var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, assetId)
              .withProjectId(projectId)
              .withResourceType(RESOURCE_TYPE)
              .withRegion(zone.getName())
              .withConfiguration(GCPUtils.asJsonNode(instance))
              .build();

            emitter.emit(create(session, List.of(fullService() + ":instance"), data.toJsonNode()));
          });

      });
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException("GCP::ComputeEngine::Instances", e);
    }
  }

  private void discoverDisks(ObjectMapper mapper, String projectId, Session session, Emitter emitter) {
    final String RESOURCE_TYPE = "GCP::ComputeEngine::Disk";

    try (var diskClient = DiskClient.create();
         var zoneClient = ZoneClient.create()) {
      // On2 - we are listing all disks in all zones
      zoneClient.listZones(projectId).iterateAll().forEach(zone -> {

        diskClient.listDisks(ProjectZoneName.of(projectId, zone.getName())).iterateAll()
          .forEach(disk -> {
            String assetId = String.format("%s::%s", disk.getName(), disk.getId());
            var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, assetId)
              .withProjectId(projectId)
              .withResourceType(RESOURCE_TYPE)
              .withRegion(zone.getName())
              .withConfiguration(GCPUtils.asJsonNode(disk))
              .build();

            emitter.emit(create(session, List.of(fullService() + ":disk"), data.toJsonNode()));
          });

      });
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException("GCP::ComputeEngine::Disk", e);
    }
  }
}
