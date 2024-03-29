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
import io.openraven.magpie.api.MagpieAwsResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.aws.elb.ElasticLoadBalancingLoadBalancer;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.elasticloadbalancing.ElasticLoadBalancingClient;
import software.amazon.awssdk.services.elasticloadbalancing.model.DescribeLoadBalancerAttributesRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.DescribeTagsRequest;
import software.amazon.awssdk.services.elasticloadbalancing.model.LoadBalancerDescription;
import software.amazon.awssdk.services.elasticloadbalancing.model.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class ELBDiscovery implements AWSDiscovery {

  private static final String SERVICE = "elb";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return ElasticLoadBalancingClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {
    final String RESOURCE_TYPE = ElasticLoadBalancingLoadBalancer.RESOURCE_TYPE;

    try (final var client = clientCreator.apply(ElasticLoadBalancingClient.builder()).build()) {
      client.describeLoadBalancers().loadBalancerDescriptions().forEach(loadBalancer -> {
        final var name = loadBalancer.loadBalancerName();
        final var arn = String.format("arn:aws:elasticloadbalancing:%s:%s:loadbalancer/%s", region, account, name);

        final var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, arn)
          .withResourceName(loadBalancer.dnsName())
          .withResourceId(name)
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(loadBalancer.toBuilder()))
          .withCreatedIso(loadBalancer.createdTime())
          .withAccountId(account)
          .withAwsRegion(region.toString())
          .build();

        discoverAttributes(client, loadBalancer, data, mapper);
        discoverTags(client, loadBalancer, data, mapper);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":loadBalancer"), data.toJsonNode()));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverAttributes(ElasticLoadBalancingClient client, LoadBalancerDescription resource, MagpieAwsResource data, ObjectMapper mapper) {
    getAwsResponse(
      () -> client.describeLoadBalancerAttributes(DescribeLoadBalancerAttributesRequest.builder().loadBalancerName(resource.loadBalancerName()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of("attributes", resp.toBuilder())),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of("attributes", noresp))
    );

  }

  private void discoverTags(ElasticLoadBalancingClient client, LoadBalancerDescription resource, MagpieAwsResource data, ObjectMapper mapper) {
    getAwsResponse(
      () -> client.describeTags(DescribeTagsRequest.builder().loadBalancerNames(resource.loadBalancerName()).build()).tagDescriptions(),
      (resp) -> {
        JsonNode tagsNode = mapper.convertValue(
          resp.stream()
            .flatMap(tagDescription -> tagDescription.tags().stream())
            .collect(Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
        AWSUtils.update(data.tags, tagsNode);
      },
      (noresp) -> AWSUtils.update(data.tags, noresp)
    );
  }
}
