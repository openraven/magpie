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
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.Conversions;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.javatuples.Pair;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.elasticsearch.ElasticsearchClient;
import software.amazon.awssdk.services.elasticsearch.model.DescribeElasticsearchDomainRequest;
import software.amazon.awssdk.services.elasticsearch.model.ElasticsearchDomainStatus;
import software.amazon.awssdk.services.elasticsearch.model.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.*;

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
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger) {
    final var client = ElasticsearchClient.builder().region(region).build();

    getAwsResponse(
      () ->  client.listDomainNames().domainNames().stream()
      .map(domainInfo -> client.describeElasticsearchDomain(DescribeElasticsearchDomainRequest.builder().build()).domainStatus()),
      (resp) -> resp.forEach(domain -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", domain.toBuilder());
        data.put("region", region.toString());

        discoverAlarmTags(client, domain, data, mapper);
        discoverSize(domain, data, region);


        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":domain"), data));
      }),
      (noresp) -> logger.error("Failed to get domains in {}", region)
    );
  }

  private void discoverAlarmTags(ElasticsearchClient client, ElasticsearchDomainStatus resource, ObjectNode data, ObjectMapper mapper) {
    var obj = data.putObject("tags");

    getAwsResponse(
      () -> client.listTags(builder -> builder.arn(resource.arn())),
      (resp) -> {
        JsonNode tagsNode = mapper.convertValue(resp.tagList().stream()
          .collect(Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
        AWSUtils.update(obj, tagsNode);
      },
      (noresp) -> AWSUtils.update(obj, noresp)
    );
  }

  private void discoverSize(ElasticsearchDomainStatus resource, ObjectNode data, Region region) {
    try {
      List<Dimension> dimensions = new ArrayList<>();
      dimensions.add(Dimension.builder().name("DomainName").value(resource.domainName()).build());
      dimensions.add(Dimension.builder().name("ClientId").value(getAwsAccountId()).build());
      Pair<Double, GetMetricStatisticsResponse> clusterUsedSpace =
        getCloudwatchDoubleMetricMaximum(region.toString(), "AWS/ES", "ClusterUsedSpace", dimensions);

      if (clusterUsedSpace.getSize() > 0) {
        final int numNodes = resource.elasticsearchClusterConfig().instanceCount();
        final int volSizeAsGB = resource.ebsOptions().volumeSize();

        data.put("clusterUsedSpace", clusterUsedSpace.getValue0());

        data.put("maxSizeInBytes", Conversions.GibToBytes(numNodes * volSizeAsGB));
        data.put("sizeInBytes", Conversions.MibToBytes(clusterUsedSpace.getValue0().longValue()));
      }
    } catch (Exception ignored) {
    }
  }
}
