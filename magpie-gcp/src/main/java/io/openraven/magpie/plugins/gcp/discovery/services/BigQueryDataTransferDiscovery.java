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
import com.google.cloud.bigquery.datatransfer.v1.DataTransferServiceClient;
import com.google.cloud.bigquery.datatransfer.v1.LocationName;
import com.google.cloud.bigquery.datatransfer.v1.TransferRun;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BigQueryDataTransferDiscovery implements GCPDiscovery {
  private static final String SERVICE = "bigQueryDataTransfer";

  // https://cloud.google.com/bigquery/docs/locations
  private static final List<String> AVAILABLE_LOCATIONS = List.of(
    "asia-east1",
    "asia-northeast1",
    "asia-northeast2",
    "asia-northeast3",
    "asia-south1",
    "asia-south2",
    "asia-southeast1",
    "asia-southeast2",
    "australia-southeast1",
    "eu",
    "europe-central2",
    "europe-north1",
    "europe-west2",
    "europe-west3",
    "europe-west4",
    "europe-west6",
    "northamerica-northeast1",
    "southamerica-east1",
    "us",
    "us-central1",
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
    final String RESOURCE_TYPE = "GCP::BigQueryDataTransfer::TransferConfig";

    try (DataTransferServiceClient client = DataTransferServiceClient.create()) {
      AVAILABLE_LOCATIONS.forEach(location -> {
        LocationName parent = LocationName.of(projectId, location);
        for (var dataSource : client.listTransferConfigs(parent).iterateAll()) {
          var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, dataSource.getName())
            .withProjectId(projectId)
            .withResourceType(RESOURCE_TYPE)
            .withRegion(location)
            .withConfiguration(GCPUtils.asJsonNode(dataSource))
            .build();

          discoverTransferRuns(client, dataSource, data);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":transferConfig"), data.toJsonNode()));
        }
      });
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverTransferRuns(DataTransferServiceClient client, com.google.cloud.bigquery.datatransfer.v1.TransferConfig dataSource, MagpieGcpResource data) {
    final String fieldName = "transferRuns";

    ArrayList<TransferRun.Builder> list = new ArrayList<>();
    client.listTransferRuns(dataSource.getName()).iterateAll()
      .forEach(device -> list.add(device.toBuilder()));

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, list));
  }
}
