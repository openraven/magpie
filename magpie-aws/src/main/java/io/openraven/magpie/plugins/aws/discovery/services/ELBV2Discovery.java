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
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import org.slf4j.Logger;
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
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger) {
    final var client = ElasticLoadBalancingV2Client.builder().region(region).build();

    getAwsResponse(
      () -> client.describeLoadBalancers().loadBalancers(),
      (resp) -> resp.forEach(loadBalancerV2 -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", loadBalancerV2.toBuilder());
        data.put("region", region.toString());

        discoverTags(client, loadBalancerV2, data, mapper);

        emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":loadBalancerV2"), data));
      }),
      (noresp) -> logger.error("Failed to get loadBalancerV2 in {}", region)
    );
  }

  private void discoverTags(ElasticLoadBalancingV2Client client, LoadBalancer resource, ObjectNode data, ObjectMapper mapper) {
    var obj = data.putObject("tags");

    getAwsResponse(
      () -> client.describeTags(DescribeTagsRequest.builder().resourceArns(resource.loadBalancerArn()).build()).tagDescriptions(),
      (resp) -> {
        JsonNode tagsNode = mapper.convertValue(
          resp.stream()
            .flatMap(tagDescription -> tagDescription.tags().stream())
            .collect(Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
        AWSUtils.update(obj, tagsNode);
      },
      (noresp) -> AWSUtils.update(obj, noresp)
    );
  }
}
