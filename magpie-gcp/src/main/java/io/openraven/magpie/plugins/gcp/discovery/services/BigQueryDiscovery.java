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
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.Table;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gcp.bigquery.BigQueryDataset;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class BigQueryDiscovery implements GCPDiscovery {
  private static final String SERVICE = "bigQuery";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger) {
    BigQuery bigQuery = BigQueryOptions.getDefaultInstance().getService();

    final String RESOURCE_TYPE = BigQueryDataset.RESOURCE_TYPE;
    bigQuery.listDatasets(projectId).iterateAll()
      .forEach(datasetProxy -> {
        Dataset datasetModel = bigQuery.getDataset(datasetProxy.getDatasetId());
        var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, datasetProxy.getGeneratedId())
          .withProjectId(projectId)
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(GCPUtils.asJsonNode(datasetModel))
          .build();

        discoverTables(bigQuery, datasetProxy, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":dataset"), data.toJsonNode()));
      });
  }

  private void discoverTables(BigQuery bigQuery, Dataset dataset, MagpieGcpResource data) {
    String fieldName = "tables";

    List<Table> tables = new ArrayList<>();
    bigQuery.listTables(dataset.getDatasetId()).iterateAll()
      .forEach(tableProxy -> {
        tables.add(bigQuery.getTable(tableProxy.getTableId()));
      });

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, tables));
  }

}
