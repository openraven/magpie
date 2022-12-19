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

package io.openraven.magpie.plugins.gcp.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.pubsub.v1.SchemaServiceClient;
import com.google.cloud.pubsub.v1.SchemaServiceSettings;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.pubsub.v1.ProjectName;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gcp.pubsub.PubSubSchema;
import io.openraven.magpie.data.gcp.pubsub.PubSubSnapshots;
import io.openraven.magpie.data.gcp.pubsub.PubSubSubscription;
import io.openraven.magpie.data.gcp.pubsub.PubSubTopic;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class PubSubDiscovery implements GCPDiscovery {
  private static final String SERVICE = "pubSub";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger, Optional<CredentialsProvider> maybeCredentialsProvider) {
    discoverSchemas(mapper, projectId, session, emitter, maybeCredentialsProvider);
    discoverTopics(mapper, projectId, session, emitter, maybeCredentialsProvider);
    discoverSubscriptionsAndSnapshots(mapper, projectId, session, emitter, maybeCredentialsProvider);
  }

  private void discoverSchemas(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Optional<CredentialsProvider> maybeCredentialsProvider) {
    final String RESOURCE_TYPE = PubSubSchema.RESOURCE_TYPE;
    var builder = SchemaServiceSettings.newBuilder();
    maybeCredentialsProvider.ifPresent(builder::setCredentialsProvider);

    try (var client = SchemaServiceClient.create(builder.build())) {
      for (var schema : client.listSchemas(ProjectName.of(projectId)).iterateAll()) {
        var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, schema.getName())
          .withProjectId(projectId)
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(GCPUtils.asJsonNode(schema))
          .build();

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":schema"), data.toJsonNode()));
      }
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverTopics(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Optional<CredentialsProvider> maybeCredentialsProvider) {
    final String RESOURCE_TYPE = PubSubTopic.RESOURCE_TYPE;
    var builder = TopicAdminSettings.newBuilder();
    maybeCredentialsProvider.ifPresent(builder::setCredentialsProvider);

    try (var client = TopicAdminClient.create(builder.build())) {
      for (var topic : client.listTopics(ProjectName.of(projectId)).iterateAll()) {
        var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, topic.getName())
          .withProjectId(projectId)
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(GCPUtils.asJsonNode(topic))
          .build();

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":topic"), data.toJsonNode()));
      }
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverSubscriptionsAndSnapshots(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Optional<CredentialsProvider> maybeCredentialsProvider) {
    var builder = SubscriptionAdminSettings.newBuilder();
    maybeCredentialsProvider.ifPresent(builder::setCredentialsProvider);
    try (var client = SubscriptionAdminClient.create(builder.build())) {
      discoverSubscriptions(mapper, projectId, session, emitter, client);
      discoverSnapshots(mapper, projectId, session, emitter, client);
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException("GCP::PubSub::SubscriptionAdmin", e);
    }
  }

  private void discoverSubscriptions(ObjectMapper mapper, String projectId, Session session, Emitter emitter,  SubscriptionAdminClient client) {
    final String RESOURCE_TYPE = PubSubSubscription.RESOURCE_TYPE;

    for (var subscription : client.listSubscriptions(ProjectName.of(projectId)).iterateAll()) {
      var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, subscription.getName())
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withConfiguration(GCPUtils.asJsonNode(subscription))
        .build();

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":subscription"), data.toJsonNode()));
    }
  }

  private void discoverSnapshots(ObjectMapper mapper, String projectId, Session session, Emitter emitter,  SubscriptionAdminClient client) {
    final String RESOURCE_TYPE = PubSubSnapshots.RESOURCE_TYPE;

    for (var snapshot : client.listSubscriptions(ProjectName.of(projectId)).iterateAll()) {
      var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, snapshot.getName())
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withConfiguration(GCPUtils.asJsonNode(snapshot))
        .build();

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":snapshot"), data.toJsonNode()));
    }
  }
}
