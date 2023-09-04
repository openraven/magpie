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
import com.google.cloud.monitoring.v3.AlertPolicyServiceClient;
import com.google.cloud.monitoring.v3.AlertPolicyServiceSettings;
import com.google.cloud.monitoring.v3.GroupServiceClient;
import com.google.cloud.monitoring.v3.GroupServiceSettings;
import com.google.cloud.monitoring.v3.ServiceMonitoringServiceClient;
import com.google.cloud.monitoring.v3.ServiceMonitoringServiceSettings;
import com.google.cloud.secretmanager.v1.ProjectName;
import com.google.monitoring.v3.AlertPolicy;
import com.google.monitoring.v3.Group;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gcp.monitoring.MonitoringAlertPolicy;
import io.openraven.magpie.data.gcp.monitoring.MonitoringGroup;
import io.openraven.magpie.data.gcp.monitoring.MonitoringService;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class MonitoringDiscovery implements GCPDiscovery {
  private static final String SERVICE = "monitoring";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger, Optional<CredentialsProvider> maybeCredentialsProvider) {
    discoverMonitoringGroups(mapper, projectId, session, emitter, maybeCredentialsProvider);
    discoverAlertPolicies(mapper, projectId, session, emitter, maybeCredentialsProvider);
    discoverServices(mapper, projectId, session, emitter, maybeCredentialsProvider);
  }

  private void discoverMonitoringGroups(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Optional<CredentialsProvider> maybeCredentialsProvider) {
    final String RESOURCE_TYPE = MonitoringGroup.RESOURCE_TYPE;
    var builder = GroupServiceSettings.newBuilder();
    maybeCredentialsProvider.ifPresent(builder::setCredentialsProvider);

    try (GroupServiceClient groupServiceClient = GroupServiceClient.create(builder.build())) {
      for (Group group : groupServiceClient.listGroups(ProjectName.of(projectId)).iterateAll()) {
        var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, group.getName())
          .withProjectId(projectId)
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(GCPUtils.asJsonNode(group))
          .build();

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":group"), data.toJsonNode()));
      }
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverAlertPolicies(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Optional<CredentialsProvider> maybeCredentialsProvider) {
    final String RESOURCE_TYPE = MonitoringAlertPolicy.RESOURCE_TYPE;
    var builder = AlertPolicyServiceSettings.newBuilder();
    maybeCredentialsProvider.ifPresent(builder::setCredentialsProvider);

    try (AlertPolicyServiceClient alertPolicyServiceClient = AlertPolicyServiceClient.create(builder.build())) {
      for (AlertPolicy alertPolicy : alertPolicyServiceClient.listAlertPolicies(ProjectName.of(projectId)).iterateAll()) {
        var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, alertPolicy.getName())
          .withProjectId(projectId)
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(GCPUtils.asJsonNode(alertPolicy))
          .build();

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":alertPolicy"), data.toJsonNode()));
      }
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverServices(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Optional<CredentialsProvider> maybeCredentialsProvider) {
    final String RESOURCE_TYPE = MonitoringService.RESOURCE_TYPE;
    var builder = ServiceMonitoringServiceSettings.newBuilder();
    maybeCredentialsProvider.ifPresent(builder::setCredentialsProvider);

    try (var serviceMonitoringServiceClient = ServiceMonitoringServiceClient.create(builder.build())) {
      for (var service : serviceMonitoringServiceClient.listServices(ProjectName.of(projectId)).iterateAll()) {
        var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, service.getName())
          .withProjectId(projectId)
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(GCPUtils.asJsonNode(service))
          .build();

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":service"), data.toJsonNode()));
      }
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }
}
