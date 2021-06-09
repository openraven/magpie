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
import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.datacatalog.v1.DataCatalogClient;
import com.google.cloud.datacatalog.v1.LocationName;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPResource;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

public class DataCatalogDiscovery implements GCPDiscovery {
  private static final String SERVICE = "dataCatalog";

  // For some reason we can't just use "-" for all location, so we iterate over them all
  private static final List<String> AVAILABLE_LOCATIONS = List.of(
    "asia",
    "asia-east1",
    "asia-east2",
    "asia-northeast1",
    "asia-northeast2",
    "asia-northeast3",
    "asia-south1",
    "asia-south2",
    "asia-southeast1",
    "asia-southeast2",
    "australia-southeast1",
    "australia-southeast2",
    "eu",
    "europe-central2",
    "europe-north1",
    "europe-west1",
    "europe-west2",
    "europe-west3",
    "europe-west4",
    "europe-west5",
    "europe-west6",
    "northamerica-northeast1",
    "northamerica-northeast2",
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

  public void discover(String projectId, ObjectMapper mapper, Session session, Emitter emitter, Logger logger) {
    final String RESOURCE_TYPE = "GCP::DataCatalog::Entry";

    try (DataCatalogClient dataCatalogClient = DataCatalogClient.create()) {
      AVAILABLE_LOCATIONS.forEach(location -> {
        try {
          String parent = LocationName.of(projectId, location).toString();
          dataCatalogClient.listEntryGroups(parent).iterateAll().forEach(entryGroup -> {
            var data = new GCPResource(entryGroup.getName(), projectId, RESOURCE_TYPE, mapper);
            data.configuration = GCPUtils.asJsonNode(entryGroup, mapper);

            emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":entry"), data.toJsonNode(mapper)));
          });
        } catch (NotFoundException ignored) {
        }
      });
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }
}
