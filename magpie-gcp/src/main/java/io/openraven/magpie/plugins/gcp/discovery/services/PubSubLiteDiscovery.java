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
import com.google.api.gax.rpc.UnimplementedException;
import com.google.cloud.pubsublite.proto.LocationName;
import com.google.cloud.pubsublite.v1.AdminServiceClient;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

public class PubSubLiteDiscovery implements GCPDiscovery {
  private static final String SERVICE = "pubSubLite";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger) {
    try (var client =  AdminServiceClient.create()) {
      discoverSubscriptions(mapper, projectId, session, emitter, client);
      discoverTopic(mapper, projectId, session, emitter, client);
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException("GCP::PubSubLite", e);
    } catch (UnimplementedException e) {
      logger.error("{} while discovering {}", e.getStatusCode(), "GCP::PubSubLite");
    }
  }

  private void discoverSubscriptions(ObjectMapper mapper, String projectId, Session session, Emitter emitter, AdminServiceClient client) {
    final String RESOURCE_TYPE = "GCP::PubSubLite::Subscription";

    var parent = LocationName.of(projectId, "-");
    for (var element : client.listSubscriptions(parent.toString()).iterateAll()) {
      var data = new MagpieResource.MagpieResourceBuilder(mapper, element.getName())
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withConfiguration(GCPUtils.asJsonNode(element))
        .build();

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":subscription"), data.toJsonNode()));
    }
  }

  private void discoverTopic(ObjectMapper mapper, String projectId, Session session, Emitter emitter, AdminServiceClient client) {
    final String RESOURCE_TYPE = "GCP::PubSubLite::Topic";

    var parent = LocationName.of(projectId, "-");
    for (var element : client.listTopics(parent.toString()).iterateAll()) {
      var data = new MagpieResource.MagpieResourceBuilder(mapper, element.getName())
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withConfiguration(GCPUtils.asJsonNode(element))
        .build();

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":topic"), data.toJsonNode()));
    }
  }
}
