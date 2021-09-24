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

package io.openraven.magpie.plugins.gcp.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.rpc.PermissionDeniedException;
import com.google.cloud.resourcemanager.v3.ProjectsClient;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.OriginPlugin;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.services.*;
import io.sentry.Sentry;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class GCPDiscoveryPlugin implements OriginPlugin<GCPDiscoveryConfig> {

  public final static String ID = "magpie.gcp.discovery";
  protected static final ObjectMapper MAPPER = GCPUtils.createObjectMapper();

  private static final List<GCPDiscovery> PER_PROJECT_DISCOVERY_LIST = List.of(
    new AccessApprovalDiscovery(),
    new AutoMLDiscovery(),
    new AssetDiscovery(),
    new BigQueryDiscovery(),
    new BigQueryReservationDiscovery(),
    new BigQueryDataTransferDiscovery(),
    new BigTableDiscovery(),
    new BillingDiscovery(),
    new ClusterDiscovery(),
    new CloudBuildDiscovery(),
    new DataprocDiscovery(),
    new ContainerAnalysisDiscovery(),
    new ComputeEngineDiscovery(),
    new DlpDiscovery(),
    new DnsDiscovery(),
    new DialogflowDiscovery(),
    new DataLabelingDiscovery(),
    new ErrorReportingDiscovery(),
    new FirewallDiscovery(),
    new SecretDiscovery(),
    new ServiceDirectoryDiscovery(),
    new StorageDiscovery(),
    new GameServicesDiscovery(),
    new SpannerDiscovery(),
    new SchedulerDiscovery(),
    new RedisDiscovery(),
    new MemcacheDiscovery(),
    new MonitoringDiscovery(),
    new MonitoringDashboardDiscovery(),
    new NetworkDiscovery(),
    new ProjectDiscovery(),
    new IamDiscovery(),
    new IoTDiscovery(),
    new LoggingDiscovery(),
    new DataCatalogDiscovery(),
    new TalentDiscovery(),
    new TasksDiscovery(),
    new TranslateDiscovery(),
    new TraceDiscovery(),
    new PubSubDiscovery(),
    new PubSubLiteDiscovery(),
    new KMSDiscovery(),
    new VisionDiscovery(),
    new OsConfigDiscovery(),
    new FunctionsDiscovery(),
    new RecaptchaEnterpriseDiscovery(),
    new WebSecurityScannerDiscovery());

  private static final List<GCPDiscovery> SINGLE_DISCOVERY_LIST = List.of(
    new ResourceManagerDiscovery());

  GCPDiscoveryConfig config;

  private Logger logger;

  @Override
  public void discover(Session session, Emitter emitter) {
    getProjectList().forEach(project -> PER_PROJECT_DISCOVERY_LIST
      .stream()
      .filter(service -> isEnabled(service.service()))
      .forEach(gcpDiscovery -> {
        try {
          logger.debug("Discovering service: {}, class: {}", gcpDiscovery.service(), gcpDiscovery.getClass());
          gcpDiscovery.discoverWrapper(MAPPER, project, session, emitter, logger);
        } catch (PermissionDeniedException permissionDeniedException) {
          logger.error("{} While discovering {} service in {}", permissionDeniedException.getMessage(), gcpDiscovery.service(), project);
        }
      }));

    SINGLE_DISCOVERY_LIST.stream()
      .filter(service -> isEnabled(service.service()))
      .forEach(service -> {
      try {
        service.discoverWrapper(MAPPER, null, session, emitter, logger);
      } catch (PermissionDeniedException permissionDeniedException) {
        logger.error("{} While discovering {} service", permissionDeniedException.getMessage(), service.service());
      }
    });
  }

  List<String> getProjectList() {
    var projects = new ArrayList<String>();

    try (ProjectsClient projectsClient = ProjectsClient.create()) {
      projectsClient.searchProjects("").iterateAll().forEach(project -> projects.add(project.getProjectId()));
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException("Project::List", e);
    }

    return  projects;
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public void init(GCPDiscoveryConfig config, Logger logger) {
    Sentry.init();

    this.logger = logger;
    this.config = config;
  }

  private boolean isEnabled(String service) {
    var enabled = config.getServices().isEmpty() || config.getServices().contains(service);
    logger.debug("{} {} per config", enabled ? "Enabling" : "Disabling", service);
    return enabled;
  }

  @Override
  public Class<GCPDiscoveryConfig> configType() {
    return GCPDiscoveryConfig.class;
  }
}
