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
import com.google.cloud.osconfig.v1.OsConfigServiceClient;
import com.google.cloud.osconfig.v1.PatchJobs;
import com.google.cloud.osconfig.v1.ProjectName;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gcp.osconfig.OsConfigPatchDeployment;
import io.openraven.magpie.data.gcp.osconfig.OsConfigPatchJob;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OsConfigDiscovery implements GCPDiscovery {
  private static final String SERVICE = "osConfig";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger) {
    try (OsConfigServiceClient client = OsConfigServiceClient.create()) {
      discoverPatchJobs(mapper, projectId, session, emitter, client);
      discoverPatchDeployments(mapper, projectId, session, emitter, client);
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException("GCP::OsConfig", e);
    }

  }

  private void discoverPatchJobs(ObjectMapper mapper, String projectId, Session session, Emitter emitter, OsConfigServiceClient client) {
    final String RESOURCE_TYPE = OsConfigPatchJob.RESOURCE_TYPE;

    for (var patchJob : client.listPatchJobs(ProjectName.of(projectId)).iterateAll()) {
      var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, patchJob.getName())
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withConfiguration(GCPUtils.asJsonNode(patchJob))
        .build();

      discoverPatchJobInstanceDetails(client, patchJob, data);

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":patchJob"), data.toJsonNode()));
    }
  }

  private void discoverPatchJobInstanceDetails(OsConfigServiceClient client, PatchJobs.PatchJob patchJob, MagpieGcpResource data) {
    final String fieldName = "instanceDetails";

    ArrayList<PatchJobs.PatchJobInstanceDetails.Builder> list = new ArrayList<>();
    client.listPatchJobInstanceDetails(patchJob.getName()).iterateAll()
      .forEach(task -> list.add(task.toBuilder()));

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, list));
  }

  private void discoverPatchDeployments(ObjectMapper mapper, String projectId, Session session, Emitter emitter, OsConfigServiceClient client) {
    final String RESOURCE_TYPE = OsConfigPatchDeployment.RESOURCE_TYPE;

    for (var patchDeployment : client.listPatchDeployments(ProjectName.of(projectId)).iterateAll()) {
      var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, patchDeployment.getName())
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withConfiguration(GCPUtils.asJsonNode(patchDeployment))
        .build();

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":patchDeployment"), data.toJsonNode()));
    }
  }
}
