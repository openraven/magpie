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
import com.google.cloud.pubsub.v1.SchemaServiceClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.pubsub.v1.ProjectName;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

public class PubSubDiscovery implements GCPDiscovery {
  private static final String SERVICE = "pubSub";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger) {
    discoverSchemas(mapper, projectId, session, emitter);
    discoverTopics(mapper, projectId, session, emitter);
    discoverSubscriptionsAndSnapshots(mapper, projectId, session, emitter);
  }

  private void discoverSchemas(ObjectMapper mapper, String projectId, Session session, Emitter emitter) {
    final String RESOURCE_TYPE = "GCP::PubSub::Schema";

    try (var client = SchemaServiceClient.create()) {
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

  private void discoverTopics(ObjectMapper mapper, String projectId, Session session, Emitter emitter) {
    final String RESOURCE_TYPE = "GCP::PubSub::Topic";

    try (var client = TopicAdminClient.create()) {
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

  private void discoverSubscriptionsAndSnapshots(ObjectMapper mapper, String projectId, Session session, Emitter emitter) {
    try (var client = SubscriptionAdminClient.create()) {
      discoverSubscriptions(mapper, projectId, session, emitter, client);
      discoverSnapshots(mapper, projectId, session, emitter, client);
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException("GCP::PubSub::SubscriptionAdmin", e);
    }
  }

  private void discoverSubscriptions(ObjectMapper mapper, String projectId, Session session, Emitter emitter,  SubscriptionAdminClient client) {
    final String RESOURCE_TYPE = "GCP::PubSub::Subscription";

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
    final String RESOURCE_TYPE = "GCP::PubSub::Snapshots";

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
