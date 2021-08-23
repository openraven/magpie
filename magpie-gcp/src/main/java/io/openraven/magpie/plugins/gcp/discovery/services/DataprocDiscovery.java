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
import com.google.cloud.dataproc.v1.*;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

public class DataprocDiscovery implements GCPDiscovery {
  private static final String SERVICE = "dataproc";

  private static final List<String> AVAILABLE_LOCATIONS = List.of(
    "asia-east1",
    "asia-east2",
    "asia-northeast1",
    "asia-northeast2",
    "asia-northeast3",
    "asia-south1",
    "asia-southeast1",
    "asia-southeast2",
    "australia-southeast1",
    "australia-southeast2",
    "europe-central2",
    "europe-north1",
    "europe-west1",
    "europe-west2",
    "europe-west3",
    "europe-west4",
    "europe-west5",
    "europe-west6",
    "northamerica-northeast1",
    "southamerica-east1",
    "us-central1",
    "us-east1",
    "us-east4",
    "us-west1",
    "us-west2",
    "us-west3",
    "us-west4",
    "global");

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger) {
    AVAILABLE_LOCATIONS.forEach(location -> {
      discoverClusters(mapper, projectId, session, emitter, location);
      discoverJobs(mapper, projectId, session, emitter, location);
    });
  }

  private void discoverClusters(ObjectMapper mapper, String projectId, Session session, Emitter emitter, String location) {
    final String RESOURCE_TYPE = "GCP::Dataproc::Cluster";

    try {
      var clusterControllerConfig = location.equals("global") ?
        ClusterControllerSettings.newBuilder().build() :
        ClusterControllerSettings.newBuilder()
          .setEndpoint("<LOCATION>-dataproc.googleapis.com:443".replace("<LOCATION>", location))
          .build();
      try (var client = ClusterControllerClient.create(clusterControllerConfig)){
        for (var cluster : client.listClusters(projectId, location).iterateAll()) {
          String assetId = String.format("%s::%s", RESOURCE_TYPE, cluster.getClusterName());
          var data = new MagpieResource.MagpieResourceBuilder(mapper, assetId)
            .withProjectId(projectId)
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(GCPUtils.asJsonNode(cluster))
            .build();

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":cluster"), data.toJsonNode()));
        }
      }
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverJobs(ObjectMapper mapper, String projectId, Session session, Emitter emitter, String location) {
    final String RESOURCE_TYPE = "GCP::Dataproc::Job";

    try {
      var clusterControllerConfig = location.equals("global") ?
        JobControllerSettings.newBuilder().build() :
        JobControllerSettings.newBuilder()
          .setEndpoint("<LOCATION>-dataproc.googleapis.com:443".replace("<LOCATION>", location))
          .build();
      try (var client = JobControllerClient.create(clusterControllerConfig)){
        for (var job : client.listJobs(projectId, location).iterateAll()) {
          String assetId = String.format("%s::%s", RESOURCE_TYPE, job.getReference().getJobId());
          var data = new MagpieResource.MagpieResourceBuilder(mapper, assetId)
            .withProjectId(projectId)
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(GCPUtils.asJsonNode(job))
            .build();

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":job"), data.toJsonNode()));
        }
      }
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }
}
