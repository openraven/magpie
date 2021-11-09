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
import com.google.appengine.repackaged.com.google.common.base.Pair;
import com.google.cloud.resourcemanager.v3.*;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

public class ResourceManagerDiscovery implements GCPDiscovery {
  private static final String SERVICE = "resourceManager";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger) {
    discoverOrganization(mapper, projectId, session, emitter);
    discoverProjects(mapper, projectId, session, emitter);
    discoverFolders(mapper, projectId, session, emitter);
  }

  private void discoverOrganization(ObjectMapper mapper, String projectId, Session session, Emitter emitter) {
    final String RESOURCE_TYPE = "GCP::ResourceManager::Organization";

    try (var client = OrganizationsClient.create()) {
      for (var organization : client.searchOrganizations("").iterateAll()) {
        var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, organization.getName())
          .withProjectId(projectId)
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(GCPUtils.asJsonNode(organization))
          .build();

        discoverOrganizationIamPolicy(client, organization, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":organization"), data.toJsonNode()));
      }
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverOrganizationIamPolicy(OrganizationsClient client, Organization organization, MagpieGcpResource data) {
    final String fieldName = "iamPolicy";

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, client.getIamPolicy(organization.getName()).toBuilder()));
  }

  private void discoverProjects(ObjectMapper mapper, String projectId, Session session, Emitter emitter) {
    final String RESOURCE_TYPE = "GCP::ResourceManager::Project";

    try (var client = ProjectsClient.create()) {
      for (var project : client.searchProjects("").iterateAll()) {
        var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, project.getName())
          .withProjectId(projectId)
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(GCPUtils.asJsonNode(project))
          .build();

        discoverProjectIamPolicy(client, project, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":project"), data.toJsonNode()));
      }
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverProjectIamPolicy(ProjectsClient client, Project project, MagpieGcpResource data) {
    final String fieldName = "iamPolicy";
    String resource = ProjectName.of(project.getProjectId()).toString();

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, client.getIamPolicy(resource).toBuilder()));
  }

  private void discoverFolders(ObjectMapper mapper, String projectId, Session session, Emitter emitter) {
    final String RESOURCE_TYPE = "GCP::ResourceManager::Folder";

    try (var client = FoldersClient.create()) {
      for (var folder : client.searchFolders("").iterateAll()) {
        var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, folder.getName())
          .withProjectId(projectId)
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(GCPUtils.asJsonNode(folder))
          .build();

        discoverFolderIamPolicy(client, folder, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":folder"), data.toJsonNode()));
      }
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverFolderIamPolicy(FoldersClient client, Folder folder, MagpieGcpResource data) {
    final String fieldName = "iamPolicy";
    String resource = ProjectName.of(folder.getName()).toString();

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, client.getIamPolicy(resource).toBuilder()));
  }
}
