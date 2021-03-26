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

package io.openraven.magpie.plugins.aws.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.DataCatalogSummary;
import software.amazon.awssdk.services.athena.model.ListDataCatalogsRequest;
import software.amazon.awssdk.services.athena.model.ListDatabasesRequest;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class AthenaDiscovery implements AWSDiscovery {

  private static final String SERVICE = "athena";

  private final List<LocalDiscovery> discoveryMethods = Collections.singletonList(
    this::discoverAttributes
  );

  @FunctionalInterface
  interface LocalDiscovery {
    void discover(AthenaClient client, DataCatalogSummary resource, ObjectNode data, ObjectMapper mapper);
  }

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger) {
    final var client = AthenaClient.builder().region(region).build();

    getAwsResponse(
      () -> client.listDataCatalogsPaginator(ListDataCatalogsRequest.builder().build()).dataCatalogsSummary(),
      (resp) -> resp.forEach(dataCatalog -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", dataCatalog.toBuilder());
        data.put("region", region.toString());

        for (var dm : discoveryMethods)
          dm.discover(client, dataCatalog, data, mapper);

        emitter.emit(new MagpieEnvelope(session, List.of(fullService()), data));
      }),
      (noresp) -> logger.error("Failed to get dataCatalogs in {}", region)
    );
  }

  private void discoverAttributes(AthenaClient client, DataCatalogSummary resource, ObjectNode data, ObjectMapper mapper) {
    final String keyname = "databases";

    getAwsResponse(
      () -> client.listDatabasesPaginator(ListDatabasesRequest.builder().catalogName(resource.catalogName()).build())
        .stream()
        .map(r -> r.toBuilder())
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }
}
