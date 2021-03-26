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
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.api.Session;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;

import java.util.List;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;
import static java.util.Arrays.asList;

public class SNSDiscovery implements AWSDiscovery {

  private static final String SERVICE = "sns";

  private final List<LocalDiscovery> discoveryMethods = asList(
    this::discoverTopics,
    this::discoverSubscriptions
  );

  @FunctionalInterface
  interface LocalDiscovery {
    void discover(SnsClient client, ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger);
  }

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger) {
    final var client = SnsClient.builder().region(region).build();

    discoveryMethods.forEach(dm -> dm.discover(client, mapper, session, region, emitter, logger));
  }

  private void discoverTopics(SnsClient client, ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger) {
    getAwsResponse(
      () -> client.listTopicsPaginator().topics().
        stream()
        .collect(Collectors.toList()),
      (resp) -> resp.forEach(topic -> getAwsResponse(
        () -> client.getTopicAttributes(GetTopicAttributesRequest.builder().topicArn(topic.topicArn()).build()),
        (attributesResp) -> {
          var data = mapper.createObjectNode();
          data.putPOJO("configuration", attributesResp.toBuilder());
          data.put("region", region.toString());

          emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":topic"), data));
        },
        (norespAttributes) -> logger.error("Failed to get topicAttributes in {}", region)
      )),
      (noresp) -> logger.error("Failed to get topic in {}", region)
    );
  }

  private void discoverSubscriptions(SnsClient client, ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger) {
    logger.trace("DS");
    getAwsResponse(
      () -> client.listSubscriptionsPaginator().subscriptions().
        stream()
        .collect(Collectors.toList()),
      (resp) -> resp.forEach(topic -> getAwsResponse(
        () -> client.getSubscriptionAttributes(GetSubscriptionAttributesRequest.builder().subscriptionArn(topic.topicArn()).build()),
        (attributesResp) -> {
          var data = mapper.createObjectNode();
          data.putPOJO("configuration", attributesResp.toBuilder());
          data.put("region", region.toString());

          emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":subscription"), data));
        },
        (norespAttributes) -> logger.error("Failed to get subscriptionAttributes in {}", region)
      )),
      (noresp) -> logger.error("Failed to get subscriptionA in {}", region)
    );
  }
}
