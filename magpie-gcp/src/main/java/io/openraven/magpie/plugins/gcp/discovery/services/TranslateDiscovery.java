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
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslationServiceClient;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

public class TranslateDiscovery implements GCPDiscovery {
  private static final String SERVICE = "translate";

  private static final List<String> AVAILABLE_LOCATIONS = List.of("us-central1", "global");

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger) {
    final String RESOURCE_TYPE = "GCP::Translate::Secret";

    try (TranslationServiceClient translationServiceClient = TranslationServiceClient.create()) {
      try {
        AVAILABLE_LOCATIONS.forEach(location -> {
          String parent = LocationName.of(projectId, location).toString();
          for (var glossary : translationServiceClient.listGlossaries(parent).iterateAll()) {
            var data = new MagpieResource.MagpieResourceBuilder(mapper, glossary.getName())
              .withProjectId(projectId)
              .withResourceType(RESOURCE_TYPE)
              .withConfiguration(GCPUtils.asJsonNode(glossary))
              .build();

            emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":glossary"), data.toJsonNode()));
          }
        });
      } catch (Exception ex) {
        DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, ex);
      }
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }
}
