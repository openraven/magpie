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
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSResource;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
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


  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return DynamoDbClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    final var client = DynamoDbClient.builder().region(region).build();

    discoverGlobalTables(mapper, session, region, emitter, logger, client, account);
    discoverTables(mapper, session, region, emitter, logger, client, account);
  }

  private void discoverGlobalTables(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, DynamoDbClient client, String account) {
    getAwsResponse(
      () -> client.listGlobalTables().globalTables().stream()
        .map(globalTable -> client.describeGlobalTable(
          DescribeGlobalTableRequest.builder().globalTableName(globalTable.globalTableName()).build()).globalTableDescription()),
      (resp) -> resp.forEach(globalTable -> {
        var data = new AWSResource(globalTable.toBuilder(), region.toString(), account, mapper);
        data.arn = globalTable.globalTableArn();
        data.resourceName = globalTable.globalTableName();
        data.resourceType = "AWS::DynamoDB::GlobalTable";

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":globalTable"), data.toJsonNode(mapper)));
      }),
      (noresp) -> logger.error("Failed to get globalTables in {}", region)
    );
  }

  private void discoverTables(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, DynamoDbClient client, String account) {
    getAwsResponse(
      () -> client.listTablesPaginator().tableNames().stream()
        .map(tableName -> client.describeTable(DescribeTableRequest.builder().tableName(tableName).build()).table()),
      (resp) -> resp.forEach(table -> {
        var data = new AWSResource(table.toBuilder(), region.toString(), account, mapper);
        data.resourceName = table.tableName();
        data.resourceId = table.tableId();
        data.arn = table.tableArn();
        data.createdIso = table.creationDateTime().toString();
        data.resourceType = "AWS::DynamoDB::Table";

        discoverContinuousBackups(client, table, data);
        discoverTags(client, table, data, mapper);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":table"), data.toJsonNode(mapper)));
      }),
      (noresp) -> logger.error("Failed to get tables in {}", region)
    );
  }

  private void discoverContinuousBackups(DynamoDbClient client, TableDescription resource, AWSResource data) {
    final String keyname = "continuousBackups";

    getAwsResponse(
      () -> client.describeContinuousBackups(builder -> builder.tableName(resource.tableName())),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverTags(DynamoDbClient client, TableDescription resource, AWSResource data, ObjectMapper mapper) {
    final String keyname = "tags";

    getAwsResponse(
      () -> client.listTagsOfResource(ListTagsOfResourceRequest.builder().resourceArn(resource.tableArn()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, mapper.convertValue(
        resp.tags().stream().collect(Collectors.toMap(Tag::key, Tag::value)), JsonNode.class))),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }
}
