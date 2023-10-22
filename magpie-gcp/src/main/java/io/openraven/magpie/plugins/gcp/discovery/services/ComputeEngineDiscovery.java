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
import com.google.cloud.compute.v1.*;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gcp.compute.ComputeDisk;
import io.openraven.magpie.data.gcp.compute.ComputeInstance;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider.create;

public class ComputeEngineDiscovery implements GCPDiscovery {
  private static final String SERVICE = "computeEngine";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger, Optional<CredentialsProvider> maybeCredentialsProvider) {

    final var diskSettings = DisksSettings.newBuilder();
    final var instancesSettings = InstancesSettings.newBuilder();
    final var zonesSettings = ZonesSettings.newBuilder();

    maybeCredentialsProvider.ifPresent(diskSettings::setCredentialsProvider);
    maybeCredentialsProvider.ifPresent(instancesSettings::setCredentialsProvider);
    maybeCredentialsProvider.ifPresent(zonesSettings::setCredentialsProvider);

    try (var diskClient = DisksClient.create(diskSettings.build());
         var instancesClient = InstancesClient.create(instancesSettings.build());
         var zoneClient = ZonesClient.create(zonesSettings.build())) {
      logger.debug("In discovery method for project={}", projectId);
      try {
        discoverInstances(mapper, projectId, session, emitter, instancesClient, zoneClient, logger);
      } catch (IOException e) {
        DiscoveryExceptions.onDiscoveryException("GCP::ComputeEngine::Instances", e);
      }
//      try {
//        discoverDisks(mapper, projectId, session, emitter, diskClient, zoneClient);
//      } catch (IOException e) {
//        DiscoveryExceptions.onDiscoveryException("GCP::ComputeEngine::Disk", e);
//      }
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException("GCP::ComputeEngine::ClientAllocation", e);
    }
  }

  private void discoverInstances( ObjectMapper mapper, String projectId, Session session, Emitter emitter, InstancesClient instancesClient, ZonesClient zoneClient, Logger logger) throws IOException {
    logger.debug("Discovering instances on project={}", projectId);
    final String RESOURCE_TYPE = ComputeInstance.RESOURCE_TYPE;

      zoneClient.list(projectId).iterateAll().forEach(zone -> {
        logger.debug("Discovering instances on project={}, zone={}", projectId, zone);
        var pages = instancesClient.listPagedCallable().call(ListInstancesRequest.newBuilder().setProject(projectId).setZone(zone.getName()).build());
        pages.iteratePages().forEach(p -> {
          logger.debug("Discovering instances on project={}, zone={}, elementCount={}", projectId, zone, p.getPageElementCount());
          for (Instance instance : p.iterateAll()) {
            logger.debug("Discovered instance={}", instance.getSelfLink());
            String assetId = instanceSelfLinkToAssetId(instance.getSelfLink());
            var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, assetId)
              .withResourceId(assetId)
              .withResourceName(instance.getName())
              .withProjectId(projectId)
              .withResourceType(RESOURCE_TYPE)
              .withRegion(zone.getName())
//              .withConfiguration(GCPUtils.asJsonNode(instance))
              .build();
            emitter.emit(create(session, List.of(fullService() + ":instance"), data.toJsonNode()));
          }
        });
      });
  }

  private void discoverDisks(ObjectMapper mapper, String projectId, Session session, Emitter emitter, DisksClient diskClient, ZonesClient zoneClient) throws IOException {
    final String RESOURCE_TYPE = ComputeDisk.RESOURCE_TYPE;

      // On2 - we are listing all disks in all zones
      zoneClient.list(projectId).iterateAll().forEach(zone -> {
        var disks = diskClient.listPagedCallable().call(ListDisksRequest.newBuilder().setProject(projectId).setZone(zone.getName()).build());
        disks.iteratePages().forEach(p -> {
          for (Disk disk : p.iterateAll()) {
            String assetId = diskSelfLinkToAssetId(disk.getSelfLink());
            var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, assetId)
              .withResourceId(assetId)
              .withResourceName(disk.getName())
              .withProjectId(projectId)
              .withResourceType(RESOURCE_TYPE)
              .withRegion(zone.getName())
              .withConfiguration(GCPUtils.asJsonNode(disk))
              .build();
            emitter.emit(create(session, List.of(fullService() + ":disk"), data.toJsonNode()));
          }
        });
      });
  }

  private String instanceSelfLinkToAssetId(String selfLink) {
    var matcher = Pattern.compile("https://.+?/compute/v\\d/projects/(.+?)/zones/(.+?)/instances/(.+)").matcher(selfLink);
    if (matcher.find()) {
      // project id, zone, instance ID
      return String.format("//compute.googleapis.com/projects/%s/zones/%s/instances/%s", matcher.group(1), matcher.group(2), matcher.group(3));
    } else {
      throw new IllegalArgumentException("Invalid selfLink: " + selfLink);
    }
  }

  private String diskSelfLinkToAssetId(String selfLink) {
    var matcher = Pattern.compile("https://.+?/compute/v\\d/projects/(.+?)/zones/(.+?)/disks/(.+)").matcher(selfLink);
    if (matcher.find()) {
      // project id, zone, instance ID
      return String.format("//compute.googleapis.com/projects/%s/zones/%s/disks/%s", matcher.group(1), matcher.group(2), matcher.group(3));
    } else {
      throw new IllegalArgumentException("Invalid selfLink: " + selfLink);
    }
  }
}
