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
import com.google.cloud.devtools.containeranalysis.v1beta1.GrafeasV1Beta1Client;
import com.google.cloud.secretmanager.v1.ProjectName;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

public class ContainerAnalysisDiscovery implements GCPDiscovery {
  private static final String SERVICE = "containerAnalysis";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger) {
    try (var client = GrafeasV1Beta1Client.create()) {
      discoverOccurrences(mapper, projectId, session, emitter, client);
      discoverNotes(mapper, projectId, session, emitter, client);
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException("GCP::ContainerAnalysis", e);
    }
  }

  private void discoverOccurrences(ObjectMapper mapper, String projectId, Session session, Emitter emitter, GrafeasV1Beta1Client client) {
    final String RESOURCE_TYPE = "GCP::ContainerAnalysis::Occurrence";

    for (var occurrence : client.listOccurrences(ProjectName.of(projectId).toString(), "").iterateAll()) {
      var data = new MagpieResource.MagpieResourceBuilder(mapper, occurrence.getName())
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withConfiguration(GCPUtils.asJsonNode(occurrence))
        .build();

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":occurrence"), data.toJsonNode()));
    }
  }
  private void discoverNotes(ObjectMapper mapper, String projectId, Session session, Emitter emitter, GrafeasV1Beta1Client client) {
    final String RESOURCE_TYPE = "GCP::ContainerAnalysis::Note";

    for (var note : client.listNotes(ProjectName.of(projectId).toString(), "").iterateAll()) {
      var data = new MagpieResource.MagpieResourceBuilder(mapper, note.getName())
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withConfiguration(GCPUtils.asJsonNode(note))
        .build();

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":note"), data.toJsonNode()));
    }
  }
}
