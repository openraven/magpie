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
import com.google.cloud.dlp.v2.DlpServiceClient;
import com.google.privacy.dlp.v2.ProjectName;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

public class DlpDiscovery implements GCPDiscovery {
  private static final String SERVICE = "dlp";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger) {
    try (DlpServiceClient dlpServiceClient = DlpServiceClient.create()) {
      discoverJobTrigger(mapper, projectId, session, emitter, dlpServiceClient);
      discoverDlpJobs(mapper, projectId, session, emitter, dlpServiceClient);
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException("Dlp", e);
    }
  }

  private void discoverJobTrigger(ObjectMapper mapper, String projectId, Session session, Emitter emitter, DlpServiceClient dlpServiceClient) {
    final String RESOURCE_TYPE = "GCP::Dlp::JobTrigger";

    for (var jobTrigger : dlpServiceClient.listJobTriggers(ProjectName.of(projectId)).iterateAll()) {
      var data = new MagpieResource.MagpieResourceBuilder(mapper, jobTrigger.getName())
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withConfiguration(GCPUtils.asJsonNode(jobTrigger))
        .build();

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":jobTrigger"), data.toJsonNode()));
    }
  }

  private void discoverDlpJobs(ObjectMapper mapper, String projectId, Session session, Emitter emitter, DlpServiceClient dlpServiceClient) {
    final String RESOURCE_TYPE = "GCP::Dlp::DlpJob";

    for (var dlpJob : dlpServiceClient.listDlpJobs(ProjectName.of(projectId)).iterateAll()) {
      var data = new MagpieResource.MagpieResourceBuilder(mapper, dlpJob.getName())
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withConfiguration(GCPUtils.asJsonNode(dlpJob))
        .build();

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":dlpJob"), data.toJsonNode()));
    }
  }
}
