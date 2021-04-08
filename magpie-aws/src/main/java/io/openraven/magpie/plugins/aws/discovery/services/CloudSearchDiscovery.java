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
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.javatuples.Pair;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudsearch.CloudSearchClient;
import software.amazon.awssdk.services.cloudsearch.model.*;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.*;

public class CloudSearchDiscovery implements AWSDiscovery {

  private static final String SERVICE = "cloudSearch";

  private final List<LocalDiscovery> discoveryMethods = List.of(
    this::discoverSuggesters,
    this::discoverServiceAccessPolicies,
    this::discoverExpressions,
    this::discoverIndexFields,
    this::discoverSize
  );

  @FunctionalInterface
  interface LocalDiscovery {
    void discover(CloudSearchClient client, DomainStatus resource, ObjectNode data, ObjectMapper mapper);
  }

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return CloudSearchClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger) {
    final var client = CloudSearchClient.builder().region(region).build();

    getAwsResponse(
      () -> client.describeDomains(DescribeDomainsRequest.builder().domainNames(client.listDomainNames().domainNames().keySet()).build())
        .domainStatusList(),
      (resp) -> resp.forEach(domain -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", domain.toBuilder());
        data.put("region", region.toString());

        for (var dm : discoveryMethods)
          dm.discover(client, domain, data, mapper);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":domain"), data));
      }),
      (noresp) -> logger.error("Failed to get domains in {}", region)
    );
  }

  private void discoverSuggesters(CloudSearchClient cloudSearchClient, DomainStatus domainStatus, ObjectNode data, ObjectMapper mapper) {
    final String keyname = "suggesters";

    getAwsResponse(
      () -> cloudSearchClient.describeSuggesters(DescribeSuggestersRequest.builder().domainName(domainStatus.domainName()).build()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private void discoverServiceAccessPolicies(CloudSearchClient cloudSearchClient, DomainStatus domainStatus, ObjectNode data, ObjectMapper mapper) {
    final String keyname = "serviceAccessPolicies";

    getAwsResponse(
      () -> cloudSearchClient.describeServiceAccessPolicies(DescribeServiceAccessPoliciesRequest.builder().domainName(domainStatus.domainName()).build()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private void discoverIndexFields(CloudSearchClient cloudSearchClient, DomainStatus domainStatus, ObjectNode data, ObjectMapper mapper) {
    final String keyname = "indexFields";

    getAwsResponse(
      () -> cloudSearchClient.describeIndexFields(DescribeIndexFieldsRequest.builder().domainName(domainStatus.domainName()).build()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private void discoverExpressions(CloudSearchClient cloudSearchClient, DomainStatus domainStatus, ObjectNode data, ObjectMapper mapper) {
    final String keyname = "expressions";

    getAwsResponse(
      () -> cloudSearchClient.describeExpressions(DescribeExpressionsRequest.builder().domainName(domainStatus.domainName()).build()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private void discoverSize(CloudSearchClient cloudSearchClient, DomainStatus domainStatus, ObjectNode data, ObjectMapper mapper) {

    List<Dimension> dimensions = new ArrayList<>();
    dimensions.add(Dimension.builder().name("DomainName").value(domainStatus.domainName()).build());
//    dimensions.add(Dimension.builder().name("ClientId").value().build());

    Pair<Double, GetMetricStatisticsResponse> IndexUtilization =
      getCloudwatchDoubleMetricMaximum(data.get("region").asText(), "AWS/CloudSearch", "IndexUtilization", dimensions);

    Pair<Long, GetMetricStatisticsResponse> SearchableDocuments =
      getCloudwatchMetricMaximum(data.get("region").asText(), "AWS/CloudSearch", "SearchableDocuments", dimensions);


    if (IndexUtilization.getValue0() != null && SearchableDocuments.getValue0() != null) {
      data.put("indexUtilization", IndexUtilization.getValue0());
      data.put("searchableDocuments", SearchableDocuments.getValue0());
    }
  }
}
