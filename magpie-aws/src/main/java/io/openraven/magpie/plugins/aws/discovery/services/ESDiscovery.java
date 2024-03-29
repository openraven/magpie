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
import io.openraven.magpie.data.aws.ess.EssDomain;
import io.openraven.magpie.plugins.aws.discovery.*;
import org.javatuples.Pair;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.elasticsearch.ElasticsearchClient;
import software.amazon.awssdk.services.elasticsearch.model.DescribeElasticsearchDomainRequest;
import software.amazon.awssdk.services.elasticsearch.model.ElasticsearchDomainStatus;
import software.amazon.awssdk.services.elasticsearch.model.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;
import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getCloudwatchDoubleMetricMaximum;

public class ESDiscovery implements AWSDiscovery {

  private static final String SERVICE = "es";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return ElasticsearchClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {
    final String RESOURCE_TYPE = EssDomain.RESOURCE_TYPE;

    try (final var client = clientCreator.apply(ElasticsearchClient.builder()).build()) {
      client.listDomainNames().domainNames().stream()
        .map(domainInfo -> client.describeElasticsearchDomain(DescribeElasticsearchDomainRequest.builder().domainName(domainInfo.domainName()).build()).domainStatus())
        .forEach(domain -> {
          var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, domain.arn())
            .withResourceName(domain.domainName())
            .withResourceId(domain.domainId())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(domain.toBuilder()))
            .withAccountId(account)
            .withAwsRegion(region.toString())
            .build();

          discoverTags(client, domain, data, mapper);
          discoverSize(domain, data, region, account, logger, clientCreator);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":domain"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverTags(ElasticsearchClient client, ElasticsearchDomainStatus resource, MagpieAwsResource data, ObjectMapper mapper) {
    getAwsResponse(
      () -> client.listTags(builder -> builder.arn(resource.arn())),
      (resp) -> {
        JsonNode tagsNode = mapper.convertValue(resp.tagList().stream()
          .collect(Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
        AWSUtils.update(data.tags, tagsNode);
      },
      (noresp) -> AWSUtils.update(data.tags, noresp)
    );
  }

  private void discoverSize(ElasticsearchDomainStatus resource, MagpieAwsResource data, Region region, String account, Logger logger, MagpieAWSClientCreator clientCreator) {
    try {
      List<Dimension> dimensions = new ArrayList<>();
      dimensions.add(Dimension.builder().name("DomainName").value(resource.domainName()).build());
      dimensions.add(Dimension.builder().name("ClientId").value(account).build());

      Pair<Double, GetMetricStatisticsResponse> clusterUsedSpace =
        getCloudwatchDoubleMetricMaximum(region.toString(), "AWS/ES", "ClusterUsedSpace", dimensions, clientCreator);

      if (clusterUsedSpace.getSize() > 0) {
        final int numNodes = resource.elasticsearchClusterConfig().instanceCount();
        final int volSizeAsGB = resource.ebsOptions().volumeSize();

        AWSUtils.update(data.supplementaryConfiguration,
          Map.of("clusterUsedSpace", clusterUsedSpace.getValue0()));

        data.maxSizeInBytes = Conversions.GibToBytes((long) numNodes * volSizeAsGB);
        data.sizeInBytes = Conversions.MibToBytes(clusterUsedSpace.getValue0().longValue());
      }
    } catch (Exception ex) {
      logger.debug("Failure on ES size discovery. Region - {}; ResourceArn - {}", region, resource.arn(),  ex);
    }
  }
}
