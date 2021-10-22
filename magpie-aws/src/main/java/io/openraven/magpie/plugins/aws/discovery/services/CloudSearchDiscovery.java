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
import software.amazon.awssdk.services.cloudsearch.CloudSearchClient;
import software.amazon.awssdk.services.cloudsearch.model.*;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.*;
import static java.lang.String.format;

public class CloudSearchDiscovery implements AWSDiscovery {

  private static final String SERVICE = "cloudSearch";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return CloudSearchClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {
    final var client = clientCreator.apply(CloudSearchClient.builder()).build();
    final String RESOURCE_TYPE = "AWS::CloudSearch::Domain";

    try {
      client.describeDomains(DescribeDomainsRequest.builder().domainNames(client.listDomainNames().domainNames().keySet()).build()).domainStatusList()
        .forEach(domain -> {
          var data = new MagpieResource.MagpieResourceBuilder(mapper, domain.arn())
            .withResourceName(domain.domainName())
            .withResourceId(domain.domainId())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(domain.toBuilder()))
            .withAccountId(account)
            .withRegion(region.toString())
            .build();

          discoverSuggesters(client, domain, data);
          discoverServiceAccessPolicies(client, domain, data);
          discoverExpressions(client, domain, data);
          discoverIndexFields(client, domain, data);
          discoverSize(domain, data, account, clientCreator);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":domain"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverSuggesters(CloudSearchClient cloudSearchClient, DomainStatus domainStatus, MagpieResource data) {
    final String keyname = "suggesters";

    getAwsResponse(
      () -> cloudSearchClient.describeSuggesters(DescribeSuggestersRequest.builder().domainName(domainStatus.domainName()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverServiceAccessPolicies(CloudSearchClient cloudSearchClient, DomainStatus domainStatus, MagpieResource data) {
    final String keyname = "serviceAccessPolicies";

    getAwsResponse(
      () -> cloudSearchClient.describeServiceAccessPolicies(DescribeServiceAccessPoliciesRequest.builder().domainName(domainStatus.domainName()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverIndexFields(CloudSearchClient cloudSearchClient, DomainStatus domainStatus, MagpieResource data) {
    final String keyname = "indexFields";

    getAwsResponse(
      () -> cloudSearchClient.describeIndexFields(DescribeIndexFieldsRequest.builder().domainName(domainStatus.domainName()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverExpressions(CloudSearchClient cloudSearchClient, DomainStatus domainStatus, MagpieResource data) {
    final String keyname = "expressions";

    getAwsResponse(
      () -> cloudSearchClient.describeExpressions(DescribeExpressionsRequest.builder().domainName(domainStatus.domainName()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverSize(DomainStatus domainStatus, MagpieResource data, String account, MagpieAWSClientCreator clientCreator) {

    List<Dimension> dimensions = new ArrayList<>();
    dimensions.add(Dimension.builder().name("DomainName").value(domainStatus.domainName()).build());
    dimensions.add(Dimension.builder().name("ClientId").value(account).build());

    Pair<Double, GetMetricStatisticsResponse> IndexUtilization =
      getCloudwatchDoubleMetricMaximum(data.region, "AWS/CloudSearch", "IndexUtilization", dimensions, clientCreator);

    Pair<Long, GetMetricStatisticsResponse> SearchableDocuments =
      getCloudwatchMetricMaximum(data.region, "AWS/CloudSearch", "SearchableDocuments", dimensions, clientCreator);


    if (IndexUtilization.getValue0() != null && SearchableDocuments.getValue0() != null) {
      AWSUtils.update(data.supplementaryConfiguration, Map.of(
        "indexUtilization", IndexUtilization.getValue0(),
        "searchableDocuments", SearchableDocuments.getValue0()));
    }
  }
}
