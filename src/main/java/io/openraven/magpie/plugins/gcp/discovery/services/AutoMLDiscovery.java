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

import com.google.api.gax.rpc.InvalidArgumentException;
import com.google.cloud.automl.v1.AutoMlClient;
import com.google.cloud.automl.v1.Dataset;
import com.google.cloud.automl.v1.Model;
import com.google.cloud.memcache.v1.LocationName;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPResource;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

public class AutoMLDiscovery implements GCPDiscovery {
  private static final String SERVICE = "autoML";

  private static final List<String> AVAILABLE_LOCATIONS = List.of("us-central1", "eu");

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(String projectId, Session session, Emitter emitter, Logger logger) {
    try (AutoMlClient client = AutoMlClient.create()) {
      AVAILABLE_LOCATIONS.forEach(location -> {
        discoverDatasets(projectId, location, session, emitter, client, logger);
        discoverModels(projectId, location, session, emitter, client, logger);
      });
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException("AutoML", e);
    }
  }

  private void discoverDatasets(String projectId, String location, Session session, Emitter emitter, AutoMlClient client, Logger logger) {
    final String RESOURCE_TYPE = "GCP::AutoML::Dataset";

    String parent = LocationName.of(projectId, location).toString();

    try {
      for (Dataset element : client.listDatasets(parent).iterateAll()) {
        var data = new GCPResource(element.getName(), projectId, RESOURCE_TYPE);
        data.configuration = GCPUtils.asJsonNode(element);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":dataset"), data.toJsonNode()));
      }
    } catch (InvalidArgumentException ignored) {
      logger.info("Invalid location {} for autoML discovery. This is expected", location);
    }
  }

  private void discoverModels(String projectId, String location, Session session, Emitter emitter, AutoMlClient client, Logger logger) {
    final String RESOURCE_TYPE = "GCP::AutoML::Model";

    String parent = LocationName.of(projectId, location).toString();

    try {
      for (Model element : client.listModels(parent).iterateAll()) {
        var data = new GCPResource(element.getName(), projectId, RESOURCE_TYPE);
        data.configuration = GCPUtils.asJsonNode(element);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":model"), data.toJsonNode()));
      }
    } catch (InvalidArgumentException ignored) {
      logger.info("Invalid location {} for autoML discovery. This is expected", location);
    }
  }
}
