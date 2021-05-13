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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.repackaged.com.google.gson.GsonBuilder;
import com.google.cloud.secretmanager.v1.ProjectName;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.GCPResource;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

public class SecretDiscovery implements  GCPDiscovery{
  private static final String SERVICE = "secret";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, Session session, Emitter emitter, Logger logger) {
    final String RESOURCE_TYPE = "GCP::SecretManager::Secret";
    final String PROJECT_ID = "oss-discovery-test";

    try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
      ProjectName projectName = ProjectName.of(PROJECT_ID);
      
      SecretManagerServiceClient.ListSecretsPagedResponse pagedResponse = client.listSecrets(projectName);

      pagedResponse
        .iterateAll()
        .forEach(
          secret -> {
            var data = new GCPResource(mapper);
            data.resourceType = RESOURCE_TYPE;
            data.arn = PROJECT_ID + ":" +  secret.getName();
            data.resourceName = secret.getName();
            data.resourceId = secret.getName();

            try {
              String secretJsonString = new GsonBuilder().setPrettyPrinting().create().toJson(secret);
              data.configuration = mapper.readValue(secretJsonString, JsonNode.class);
            } catch (JsonProcessingException e) {
              e.printStackTrace();
            }

            emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":secret"), data.toJsonNode(mapper)));
          });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


}
