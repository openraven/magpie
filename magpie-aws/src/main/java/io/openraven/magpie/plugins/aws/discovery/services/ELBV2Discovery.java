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
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTagsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancer;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Tag;

import java.util.List;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class ELBV2Discovery implements AWSDiscovery {

  private static final String SERVICE = "elbv2";


  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return ElasticLoadBalancingV2Client.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    final var client = AWSUtils.configure(ElasticLoadBalancingV2Client.builder(), region);
    final String RESOURCE_TYPE = "AWS::ElasticLoadBalancingV2::LoadBalancer";

    try {
      client.describeLoadBalancers().loadBalancers().forEach(loadBalancerV2 -> {
        var data = new MagpieResource.MagpieResourceBuilder(mapper, loadBalancerV2.loadBalancerArn())
          .withResourceName(loadBalancerV2.dnsName())
          .withResourceId(loadBalancerV2.loadBalancerName())
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(loadBalancerV2.toBuilder()))
          .withCreatedIso(loadBalancerV2.createdTime())
          .withAccountId(account)
          .withRegion(region.toString())
          .build();

        discoverTags(client, loadBalancerV2, data, mapper);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":loadBalancerV2"), data.toJsonNode()));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverTags(ElasticLoadBalancingV2Client client, LoadBalancer resource, MagpieResource data, ObjectMapper mapper) {
    getAwsResponse(
      () -> client.describeTags(DescribeTagsRequest.builder().resourceArns(resource.loadBalancerArn()).build()).tagDescriptions(),
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
