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
import io.openraven.magpie.api.MagpieAwsResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.aws.dynamodb.DynamoDbGlobalTable;
import io.openraven.magpie.data.aws.dynamodb.DynamoDbTable;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeGlobalTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTagsOfResourceRequest;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.model.Tag;

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
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {
    try (final var client = clientCreator.apply(DynamoDbClient.builder()).build()) {
      discoverGlobalTables(mapper, session, region, emitter, client, account);
      discoverTables(mapper, session, region, emitter, client, account, clientCreator, logger);
    }
  }

  protected void discoverGlobalTables(ObjectMapper mapper, Session session, Region region, Emitter emitter, DynamoDbClient client, String account) {
    final String RESOURCE_TYPE = DynamoDbGlobalTable.RESOURCE_TYPE;
    try {
      client.listGlobalTables().globalTables().stream()
        .map(globalTable -> client.describeGlobalTable(
          DescribeGlobalTableRequest.builder().globalTableName(globalTable.globalTableName()).build()).globalTableDescription())
        .forEach(globalTable -> {
          var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, globalTable.globalTableArn())
            .withResourceName(globalTable.globalTableName())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(globalTable.toBuilder()))
            .withAccountId(account)
            .withAwsRegion(region.toString())
            .build();

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":globalTable"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  protected void discoverTables(ObjectMapper mapper, Session session, Region region, Emitter emitter, DynamoDbClient client, String account, MagpieAWSClientCreator clientCreator, Logger logger) {
    final String RESOURCE_TYPE = DynamoDbTable.RESOURCE_TYPE;

    try {
      client.listTablesPaginator().tableNames().stream()
        .map(tableName -> client.describeTable(DescribeTableRequest.builder().tableName(tableName).build()).table())
        .forEach(table -> {
          var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, table.tableArn())
            .withResourceName(table.tableName())
            .withResourceId(table.tableId())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(table.toBuilder()))
            .withCreatedIso(table.creationDateTime())
            .withAccountId(account)
            .withAwsRegion(region.toString())
            .build();

          discoverContinuousBackups(client, table, data);
          discoverTags(client, table, data, mapper);
          discoverBackupJobs(table.tableArn(), region, data, clientCreator, logger);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":table"), data.toJsonNode()));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverContinuousBackups(DynamoDbClient client, TableDescription resource, MagpieAwsResource data) {
    final String keyname = "continuousBackups";

    getAwsResponse(
      () -> client.describeContinuousBackups(builder -> builder.tableName(resource.tableName())),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverTags(DynamoDbClient client, TableDescription resource, MagpieAwsResource data, ObjectMapper mapper) {
    final String keyname = "tags";

    getAwsResponse(
      () -> client.listTagsOfResource(ListTagsOfResourceRequest.builder().resourceArn(resource.tableArn()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, mapper.convertValue(
        resp.tags().stream().collect(Collectors.toMap(Tag::key, Tag::value)), JsonNode.class))),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }
}
