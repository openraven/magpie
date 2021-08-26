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
import com.google.cloud.logging.v2.ConfigClient;
import com.google.cloud.logging.v2.MetricsClient;
import com.google.logging.v2.LocationName;
import com.google.logging.v2.ProjectName;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

public class LoggingDiscovery implements GCPDiscovery {
  private static final String SERVICE = "logging";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger) {
    discoverMetrics(mapper, projectId, session, emitter);
    discoverConfigClientResources(mapper, projectId, session, emitter);
  }

  private void discoverMetrics(ObjectMapper mapper, String projectId, Session session, Emitter emitter) {
    final String RESOURCE_TYPE = "GCP::Logging::Metric";

    try (MetricsClient metricsClient = MetricsClient.create()) {
      String parent = ProjectName.of(projectId).toString();
      for (var metric : metricsClient.listLogMetrics(parent).iterateAll()) {
        var data = new MagpieResource.MagpieResourceBuilder(mapper, metric.getName())
          .withProjectId(projectId)
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(GCPUtils.asJsonNode(metric))
          .build();

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":metric"), data.toJsonNode()));
      }
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverConfigClientResources(ObjectMapper mapper, String projectId, Session session, Emitter emitter) {
    try (ConfigClient configClient = ConfigClient.create()) {
      discoverSinks(mapper, projectId, session, emitter, configClient);
      discoverBuckets(mapper, projectId, session, emitter, configClient);
      discoverExclusions(mapper, projectId, session, emitter, configClient);
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException("GCP::Logging::ConfigClient", e);
    }
  }

  private void discoverSinks(ObjectMapper mapper, String projectId, Session session, Emitter emitter, ConfigClient configClient) {
    final String RESOURCE_TYPE = "GCP::Logging::Sink";

    ProjectName parent = ProjectName.of(projectId);
    for (var sink : configClient.listSinks(parent).iterateAll()) {
      var data = new MagpieResource.MagpieResourceBuilder(mapper, sink.getName())
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withConfiguration(GCPUtils.asJsonNode(sink))
        .build();

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":sink"), data.toJsonNode()));
    }
  }

  private void discoverBuckets(ObjectMapper mapper, String projectId, Session session, Emitter emitter, ConfigClient configClient) {
    final String RESOURCE_TYPE = "GCP::Logging::Bucket";

    var parent = LocationName.of(projectId, "-");
    for (var bucket : configClient.listBuckets(parent).iterateAll()) {
      var data = new MagpieResource.MagpieResourceBuilder(mapper, bucket.getName())
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withConfiguration(GCPUtils.asJsonNode(bucket))
        .build();

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":bucket"), data.toJsonNode()));
    }
  }

  private void discoverExclusions(ObjectMapper mapper, String projectId, Session session, Emitter emitter, ConfigClient configClient) {
    final String RESOURCE_TYPE = "GCP::Logging::Exclusion";

    var parent = ProjectName.of(projectId);
    for (var exclusion : configClient.listExclusions(parent).iterateAll()) {
      var data = new MagpieResource.MagpieResourceBuilder(mapper, exclusion.getName())
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withConfiguration(GCPUtils.asJsonNode(exclusion))
        .build();

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":exclusion"), data.toJsonNode()));
    }
  }
}
