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
import com.google.api.gax.rpc.InvalidArgumentException;
import com.google.cloud.scheduler.v1beta1.CloudSchedulerClient;
import com.google.cloud.scheduler.v1beta1.LocationName;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

public class SchedulerDiscovery implements GCPDiscovery {
  private static final String SERVICE = "scheduler";

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
    "europe-central2",
    "europe-west1",
    "europe-west2",
    "europe-west3",
    "europe-west5",
    "europe-west6",
    "southamerica-east1",
    "us-central1",
    "us-central2",
    "us-east1",
    "us-east4",
    "us-west1",
    "us-west2",
    "us-west3",
    "us-west4");

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger) {
    final String RESOURCE_TYPE = "GCP::Scheduler::Job";

    try (var cloudSchedulerClient = CloudSchedulerClient.create()) {
      AVAILABLE_LOCATIONS.forEach(location -> {
        try {
          var parent = LocationName.of(projectId, location);
          for (var job : cloudSchedulerClient.listJobs(parent.toString()).iterateAll()) {
            var data = new MagpieResource.MagpieResourceBuilder(mapper, job.getName())
              .withProjectId(projectId)
              .withResourceType(RESOURCE_TYPE)
              .withConfiguration(GCPUtils.asJsonNode(job))
              .build();


            emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":job"), data.toJsonNode()));
          }
        } catch(InvalidArgumentException ignored) {
        }
      });

    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }
}
