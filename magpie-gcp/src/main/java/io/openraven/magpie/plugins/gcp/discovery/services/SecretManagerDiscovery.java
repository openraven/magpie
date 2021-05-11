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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.cloud.secretmanager.v1.*;
import com.google.protobuf.MapField;
import com.google.protobuf.Timestamp;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.GCPResource;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class SecretManagerDiscovery {

  private static final String SERVICE = "secretManager";

  public void discover(ObjectMapper mapper, Session session, Emitter emitter, Logger logger) {
    final String RESOURCE_TYPE = "GCP::SecretManager::Secret";

    try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
      ProjectName projectName = ProjectName.of("oss-discovery-test");
      
      SecretManagerServiceClient.ListSecretsPagedResponse pagedResponse = client.listSecrets(projectName);
      
      pagedResponse
        .iterateAll()
        .forEach(
          secret -> {
            Secret s;
            var data = new GCPResource(mapper);
            data.resourceType = RESOURCE_TYPE;
            data.arn = "oss-discovery-test" +  secret.getName();
            data.resourceName = secret.getName();
            data.resourceId = secret.getName();
            data.awsAccountId = secret.toString();

            System.out.printf("Secret %s\n", secret);

            emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(":secret"), data.toJsonNode(mapper)));
          });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
