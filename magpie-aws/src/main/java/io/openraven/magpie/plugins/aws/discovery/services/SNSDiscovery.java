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
import io.openraven.magpie.api.MagpieAwsResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.aws.sns.SNSSubscription;
import io.openraven.magpie.data.aws.sns.SNSTopic;
import io.openraven.magpie.plugins.aws.discovery.AWSDiscoveryConfig;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;

import java.util.List;

public class SNSDiscovery implements AWSDiscovery {

  private static final String SERVICE = "sns";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return SnsClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator, AWSDiscoveryConfig config) {
    try (final var client = clientCreator.apply(SnsClient.builder()).build()) {
      discoverTopics(client, mapper, session, region, emitter);
      discoverSubscriptions(client, mapper, session, region, emitter);
    }
  }

  private void discoverTopics(SnsClient client, ObjectMapper mapper, Session session, Region region, Emitter emitter) {
    final String RESOURCE_TYPE = SNSTopic.RESOURCE_TYPE;

    try {
      client.listTopics().topics().stream()
        .map(topic -> client.getTopicAttributes(GetTopicAttributesRequest.builder().topicArn(topic.topicArn()).build()))
        .forEach(attributesResp -> {
          var attributes = attributesResp.attributes();
          var arn = attributes.get("TopicArn");

          var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, attributes.get("TopicArn"))
            .withResourceName(attributes.get("DisplayName"))
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(attributesResp.toBuilder()))
            .withAccountId(attributesResp.attributes().get("Owner"))
            .withAwsRegion(Arn.fromString(arn).region().orElse(null))
            .build();

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":topic"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverSubscriptions(SnsClient client, ObjectMapper mapper, Session session, Region region, Emitter emitter) {
    final String RESOURCE_TYPE = SNSSubscription.RESOURCE_TYPE;

    try {
      client.listSubscriptions().subscriptions().stream()
        .map(subscription -> client.getSubscriptionAttributes(GetSubscriptionAttributesRequest.builder().subscriptionArn(subscription.subscriptionArn()).build()))
        .forEach(attributesResp -> {
          var attributes = attributesResp.attributes();
          var arn = Arn.fromString(attributes.get("SubscriptionArn"));

          var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, arn.toString())
            .withResourceName(getName(arn))
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(attributesResp.toBuilder()))
            .withAccountId(arn.accountId().orElse(null))
            .withAwsRegion(arn.region().orElse(null))
            .build();

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":subscription"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private String getName(Arn arn) {
    final var res = arn.resource();
    return res.resourceType().map(resourceType -> resourceType + ":" + res.resource()).orElse(null);
  }
}
