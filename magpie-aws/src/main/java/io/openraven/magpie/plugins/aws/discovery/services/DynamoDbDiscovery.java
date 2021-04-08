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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.VersioningEmitterWrapper;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class DynamoDbDiscovery implements AWSDiscovery {

  private static final String SERVICE = "dynamoDb";

  private final List<LocalDiscovery> discoveryMethods = List.of(
    this::discoverTables,
    this::discoverGlobalTables
  );

  @FunctionalInterface
  interface LocalDiscovery {
    void discover(ObjectMapper mapper, Session session, Region region, VersioningEmitterWrapper emitter, Logger logger, DynamoDbClient client);
  }

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return DynamoDbClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, VersioningEmitterWrapper emitter, Logger logger) {
    final var client = DynamoDbClient.builder().region(region).build();

    discoveryMethods.forEach(dm -> dm.discover(mapper,session,region,emitter,logger, client));
  }

  private void discoverGlobalTables(ObjectMapper mapper, Session session, Region region, VersioningEmitterWrapper emitter, Logger logger, DynamoDbClient client) {
    getAwsResponse(
      () -> client.listGlobalTables().globalTables().stream()
        .map(globalTable -> client.describeGlobalTable(
          DescribeGlobalTableRequest.builder().globalTableName(globalTable.globalTableName()).build()).globalTableDescription()),
      (resp) -> resp.forEach(globalTable -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", globalTable.toBuilder());
        data.put("region", region.toString());


        emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":globalTable"), data));
      }),
      (noresp) -> logger.error("Failed to get globalTables in {}", region)
    );
  }

  private void discoverTables(ObjectMapper mapper, Session session, Region region, VersioningEmitterWrapper emitter, Logger logger, DynamoDbClient client) {
    getAwsResponse(
      () -> client.listTablesPaginator().tableNames().stream()
        .map(tableName -> client.describeTable(DescribeTableRequest.builder().tableName(tableName).build())),
      (resp) -> resp.forEach(table -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", table.table().toBuilder());
        data.put("region", region.toString());

        discoverContinuousBackups(client, table.table(), data);
        discoverTags(client, table.table(), data, mapper);

        emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":table"), data));
      }),
      (noresp) -> logger.error("Failed to get tables in {}", region)
    );
  }


  private void discoverContinuousBackups(DynamoDbClient client, TableDescription resource, ObjectNode data) {
    final String keyname = "continuousBackups";

    getAwsResponse(
      () -> client.describeContinuousBackups(builder -> builder.tableName(resource.tableName())),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private void discoverTags(DynamoDbClient client, TableDescription resource, ObjectNode data, ObjectMapper mapper) {
    final String keyname = "tags";

    getAwsResponse(
      () -> client.listTagsOfResource(ListTagsOfResourceRequest.builder().resourceArn(resource.tableArn()).build()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, mapper.convertValue(
        resp.tags().stream().collect(Collectors.toMap(Tag::key, Tag::value)), JsonNode.class))),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }
}
