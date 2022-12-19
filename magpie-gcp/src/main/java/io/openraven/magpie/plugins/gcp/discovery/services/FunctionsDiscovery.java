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
import com.google.cloud.functions.v1.CloudFunctionsServiceClient;
import com.google.cloud.functions.v1.CloudFunctionsServiceSettings;
import com.google.cloud.functions.v1.ListFunctionsRequest;
import com.google.cloud.functions.v1.LocationName;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gcp.function.Function;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class FunctionsDiscovery implements GCPDiscovery {
  private static final String SERVICE = "functions";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger, Optional<CredentialsProvider> maybeCredentialsProvider) {
    final String RESOURCE_TYPE = Function.RESOURCE_TYPE;
    var builder = CloudFunctionsServiceSettings.newBuilder();
    maybeCredentialsProvider.ifPresent(builder::setCredentialsProvider);

    try (CloudFunctionsServiceClient clusterManagerClient = CloudFunctionsServiceClient.create(builder.build())) {
      var response = clusterManagerClient.listFunctions(
        ListFunctionsRequest.newBuilder().
          setParent(LocationName.of(projectId, "-").toString())
          .build());

      response.iterateAll()
        .forEach(function -> {
          var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, function.getName())
            .withProjectId(projectId)
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(GCPUtils.asJsonNode(function))
            .build();

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":function"), data.toJsonNode()));
        });
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }
}
