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
import com.google.appengine.repackaged.com.google.common.base.Pair;
import com.google.cloud.iam.admin.v1.IAMClient;
import com.google.cloud.iam.admin.v1.IAMSettings;
import com.google.iam.admin.v1.ListRolesRequest;
import com.google.iam.admin.v1.ServiceAccount;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gcp.account.GcpIamRole;
import io.openraven.magpie.data.gcp.account.GcpIamServiceAccount;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;

public class IamDiscovery implements GCPDiscovery {
  private static final String SERVICE = "iam";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger, Optional<CredentialsProvider> maybeCredentialsProvider) {
    try {
      final var builder = IAMSettings.newBuilder();
      maybeCredentialsProvider.ifPresent(builder::setCredentialsProvider);

      try (final var client = IAMClient.create(builder.build())) {
        discoverServiceAccounts(client, mapper, projectId, session, emitter, logger);
        discoverRoles(client, mapper, projectId, session, emitter, logger);
      }
    } catch (GeneralSecurityException | IOException e) {
      logger.error("Unable to finish IAM discovery, due to:", e);
    }
  }

  public void discoverServiceAccounts(IAMClient client, ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger) throws GeneralSecurityException, IOException {
    final String RESOURCE_TYPE = GcpIamServiceAccount.RESOURCE_TYPE;
    client.listServiceAccounts(com.google.iam.admin.v1.ProjectName.of(projectId)).iterateAll().forEach(sa -> {
      var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, sa.getName())
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withResourceName(sa.getName())
        .withConfiguration(GCPUtils.asJsonNode(sa.toBuilder()))
        .build();

      try {
        discoverServiceAccountIamPolicy(client, sa, data);
        discoverServiceAccountKeys(client, sa, data);
      } catch (IOException ex) {
        logger.warn("Couldn't discover ServiceAccount policy", ex);
      }

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":serviceAccount"), data.toJsonNode()));
    });
  }

  private void discoverServiceAccountIamPolicy(IAMClient client, ServiceAccount serviceAccount, MagpieGcpResource data) throws IOException {
    final String fieldName = "iamPolicy";
    final var policy = client.getIamPolicy(serviceAccount.getName());

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, policy.toBuilder()));
  }
  private void discoverServiceAccountKeys(IAMClient client, ServiceAccount serviceAccount, MagpieGcpResource data) throws IOException {
    final String fieldName = "keys";
    final var keys = client.listServiceAccountKeys(serviceAccount.getName(), List.of());  // Empty list requests all key types

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, keys.toBuilder()));
  }

  public void discoverRoles(IAMClient client, ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger) throws GeneralSecurityException, IOException {
    final String RESOURCE_TYPE = GcpIamRole.RESOURCE_TYPE;

    client.listRoles(ListRolesRequest.newBuilder().setParent("projects/" + projectId).build()).iterateAll().forEach(role -> {
      var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, role.getName())
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withConfiguration(GCPUtils.asJsonNode(role.toBuilder()))
        .build();

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":role"), data.toJsonNode()));
    });
  }
}
