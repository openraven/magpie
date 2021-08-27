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
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.IamScopes;
import com.google.api.services.iam.v1.model.ListRolesResponse;
import com.google.api.services.iam.v1.model.ListServiceAccountsResponse;
import com.google.api.services.iam.v1.model.ServiceAccount;
import com.google.appengine.repackaged.com.google.common.base.Pair;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.resourcemanager.v3.ProjectName;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class IamDiscovery implements GCPDiscovery {
  private static final String SERVICE = "iam";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger) {
    try {
      Iam iamService = initService();

      discoverServiceAccounts(iamService, mapper, projectId, session, emitter);
      discoverRoles(iamService, mapper, projectId, session, emitter);
    } catch (GeneralSecurityException | IOException e) {
      logger.error("Unable to finish IAM discovery, due to:", e);
    }
  }

  public void discoverServiceAccounts(Iam iamService, ObjectMapper mapper, String projectId, Session session, Emitter emitter) throws GeneralSecurityException, IOException {
    final String RESOURCE_TYPE = "GCP::Iam::ServiceAccount";

    var request = iamService.projects().serviceAccounts().list(ProjectName.of(projectId).toString());

    ListServiceAccountsResponse response;
    do {
      response = request.execute();
      if (response.getAccounts() == null) {
        continue;
      }
      for (var serviceAccount : response.getAccounts()) {
        var data = new MagpieResource.MagpieResourceBuilder(mapper, serviceAccount.getName())
          .withProjectId(projectId)
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(GCPUtils.asJsonNode(serviceAccount))
          .build();

        discoverServiceAccountIamPolicy(iamService, serviceAccount, data);
        discoverServiceAccountKeys(iamService, serviceAccount, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":serviceAccount"), data.toJsonNode()));
      }
      request.setPageToken(response.getNextPageToken());
    } while (response.getNextPageToken() != null);
  }

  private void discoverServiceAccountIamPolicy(Iam iamService, ServiceAccount serviceAccount, MagpieResource data) throws IOException {
    Iam.Projects.ServiceAccounts.GetIamPolicy getIamPolicyRequest =
      iamService.projects().serviceAccounts().getIamPolicy(serviceAccount.getName());

    final String fieldName = "iamPolicy";

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, getIamPolicyRequest.execute()));
  }
  private void discoverServiceAccountKeys(Iam iamService, ServiceAccount serviceAccount, MagpieResource data) throws IOException {
    final String fieldName = "keys";

    var request = iamService.projects().serviceAccounts().keys().list(serviceAccount.getName());
    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, request.execute()));
  }

  public void discoverRoles(Iam iamService, ObjectMapper mapper, String projectId, Session session, Emitter emitter) throws GeneralSecurityException, IOException {
    final String RESOURCE_TYPE = "GCP::Iam::Role";

    var request = iamService.roles().list();

    ListRolesResponse response;
    do {
      response = request.execute();
      if (response.getRoles() == null) {
        continue;
      }
      for (var serviceAccount : response.getRoles()) {
        var data = new MagpieResource.MagpieResourceBuilder(mapper, serviceAccount.getName())
          .withProjectId(projectId)
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(GCPUtils.asJsonNode(serviceAccount))
          .build();


        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":role"), data.toJsonNode()));
      }
      request.setPageToken(response.getNextPageToken());
    } while (response.getNextPageToken() != null);
  }

  private static Iam initService() throws GeneralSecurityException, IOException {
    GoogleCredentials credential =
      GoogleCredentials.getApplicationDefault()
        .createScoped(Collections.singleton(IamScopes.CLOUD_PLATFORM));

    return new Iam.Builder(
      GoogleNetHttpTransport.newTrustedTransport(),
      JacksonFactory.getDefaultInstance(),
      new HttpCredentialsAdapter(credential))
      .setApplicationName("service-accounts")
      .build();
  }
}
