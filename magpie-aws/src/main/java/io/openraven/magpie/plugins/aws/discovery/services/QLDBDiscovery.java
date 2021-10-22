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
import io.openraven.magpie.api.MagpieResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.javatuples.Pair;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.qldb.QldbClient;
import software.amazon.awssdk.services.qldb.model.DescribeLedgerRequest;
import software.amazon.awssdk.services.qldb.model.DescribeLedgerResponse;
import software.amazon.awssdk.services.qldb.model.ListJournalKinesisStreamsForLedgerRequest;
import software.amazon.awssdk.services.qldb.model.ListJournalKinesisStreamsForLedgerResponse;
import software.amazon.awssdk.services.qldb.model.ListJournalS3ExportsForLedgerRequest;
import software.amazon.awssdk.services.qldb.model.ListJournalS3ExportsForLedgerResponse;
import software.amazon.awssdk.services.qldb.model.ListLedgersRequest;
import software.amazon.awssdk.services.qldb.model.ListTagsForResourceRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;
import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getCloudwatchMetricMaximum;

public class QLDBDiscovery implements AWSDiscovery {

  private static final String SERVICE = "qldb";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return QldbClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {
    final var client = clientCreator.apply(QldbClient.builder()).build();
    final String RESOURCE_TYPE = "AWS::Qldb::Ledger";

    try {
      client.listLedgersPaginator(ListLedgersRequest.builder().build()).stream()
        .forEach(ledgerList -> ledgerList.ledgers()
          .stream()
          .map(ledgerSummary -> client.describeLedger(DescribeLedgerRequest.builder().name(ledgerSummary.name()).build()))
          .forEach(ledger -> {
            var data = new MagpieResource.MagpieResourceBuilder(mapper, ledger.arn())
              .withResourceName(ledger.name())
              .withResourceType(RESOURCE_TYPE)
              .withConfiguration(mapper.valueToTree(ledger.toBuilder()))
              .withCreatedIso(ledger.creationDateTime())
              .withAccountId(account)
              .withRegion(region.toString())
              .build();

            discoverStreams(client, ledger, data);
            discoverJournalS3Exports(client, ledger, data);
            discoverTags(client, ledger, data, mapper);
            discoverSize(ledger, data, region, clientCreator);

            emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":ledger"), data.toJsonNode()));
          }));
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverStreams(QldbClient client, DescribeLedgerResponse resource, MagpieResource data) {
    final String keyname = "streams";

    getAwsResponse(
      () -> client.listJournalKinesisStreamsForLedgerPaginator(ListJournalKinesisStreamsForLedgerRequest.builder().ledgerName(resource.name()).build())
        .stream()
        .map(ListJournalKinesisStreamsForLedgerResponse::toBuilder)
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverJournalS3Exports(QldbClient client, DescribeLedgerResponse resource, MagpieResource data) {
    final String keyname = "journalS3Exports";

    getAwsResponse(
      () -> client.listJournalS3ExportsForLedgerPaginator(ListJournalS3ExportsForLedgerRequest.builder().name(resource.name()).build())
        .stream()
        .map(ListJournalS3ExportsForLedgerResponse::toBuilder)
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverTags(QldbClient client, DescribeLedgerResponse resource, MagpieResource data, ObjectMapper mapper) {
    final String keyname = "tags";

    getAwsResponse(
      () -> client.listTagsForResource(ListTagsForResourceRequest.builder().resourceArn(resource.arn()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, mapper.convertValue(resp.tags(), JsonNode.class))),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverSize(DescribeLedgerResponse resource, MagpieResource data, Region region, MagpieAWSClientCreator clientCreator) {

    List<Dimension> dimensions = new ArrayList<>();
    dimensions.add(Dimension.builder().name("LedgerName").value(resource.name()).build());

    Pair<Long, GetMetricStatisticsResponse> clusterSize =
      getCloudwatchMetricMaximum(region.toString(), "AWS/QLDB", "JournalStorage", dimensions, clientCreator);

    if (clusterSize.getValue0() != null) {
      AWSUtils.update(data.supplementaryConfiguration, Map.of("JournalStorage", clusterSize.getValue0()));
      data.sizeInBytes = clusterSize.getValue0();
    }
  }
}
