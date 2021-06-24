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

import com.amazonaws.arn.Arn;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSResource;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
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
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    final var client = AWSUtils.configure(SnsClient.builder(), region);

    discoverTopics(client, mapper, session, region, emitter, account);
    discoverSubscriptions(client, mapper, session, region, emitter, account);
  }

  private void discoverTopics(SnsClient client, ObjectMapper mapper, Session session, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::SNS::Topic";

    try {
      client.listTopics().topics().stream()
        .map(topic -> client.getTopicAttributes(GetTopicAttributesRequest.builder().topicArn(topic.topicArn()).build()))
        .forEach(attributesResp -> {
          var attributes = attributesResp.attributes();
          var arn = attributes.get("TopicArn");

          var data = new AWSResource(attributesResp.toBuilder(), region.toString(), account, mapper);
          data.awsRegion = Arn.fromString(arn).getRegion();
          data.awsAccountId = attributesResp.attributes().get("Owner");
          data.arn = attributes.get("TopicArn");
          data.resourceName = attributes.get("DisplayName");
          data.resourceType = RESOURCE_TYPE;

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":topic"), data.toJsonNode(mapper)));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverSubscriptions(SnsClient client, ObjectMapper mapper, Session session, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::SNS::Subscription";

    try {
      client.listSubscriptions().subscriptions().stream()
        .map(subscription -> client.getSubscriptionAttributes(GetSubscriptionAttributesRequest.builder().subscriptionArn(subscription.subscriptionArn()).build()))
        .forEach(attributesResp -> {
          var data = new AWSResource(attributesResp.toBuilder(), region.toString(), account, mapper);
          var attributes = attributesResp.attributes();
          var arn = Arn.fromString(attributes.get("SubscriptionArn"));
          data.awsRegion = arn.getRegion();
          data.awsAccountId = arn.getAccountId();
          data.arn = arn.toString();
          data.resourceName = getName(arn);
          data.resourceType = RESOURCE_TYPE;

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":subscription"), data.toJsonNode(mapper)));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private String getName(Arn arn) {
    final var res = arn.getResource();
    return res.getResourceType() + ":" + res.getResource();
  }
}
