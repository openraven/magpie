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
import io.openraven.magpie.plugins.gcp.discovery.services.AccessApprovalDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.AssetDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.AutoMLDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.BigQueryDataTransferDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.BigQueryDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.BigQueryReservationDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.BigTableDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.BillingDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.CloudBuildDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.ClusterDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.ComputeEngineDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.ContainerAnalysisDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.DataCatalogDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.DataLabelingDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.DataprocDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.DialogflowDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.DlpDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.DnsDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.ErrorReportingDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.FirewallDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.FunctionsDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.GCPDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.GameServicesDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.IamDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.IoTDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.KMSDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.LoggingDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.MemcacheDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.MonitoringDashboardDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.MonitoringDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.NetworkDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.OsConfigDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.ProjectDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.PubSubDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.PubSubLiteDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.RecaptchaEnterpriseDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.RedisDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.ResourceManagerDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.SchedulerDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.SecretDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.ServiceDirectoryDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.SpannerDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.SqlDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.StorageDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.TasksDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.TenantDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.TraceDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.TranslateDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.VisionDiscovery;
import io.openraven.magpie.plugins.gcp.discovery.services.WebSecurityScannerDiscovery;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


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
    new SqlDiscovery(),
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
    new TenantDiscovery(),
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
          gcpDiscovery.discoverWrapper(MAPPER, project, session, emitter, logger, Optional.ofNullable(config.getCredentialsProvider()));
        } catch (Exception ex) {
          logger.error("Discovery error in service {} - {}", gcpDiscovery.service(), ex.getMessage());
          logger.debug("Details", ex);
        }
      }));

    SINGLE_DISCOVERY_LIST.stream()
      .filter(service -> isEnabled(service.service()))
      .forEach(service -> {
      try {
        service.discoverWrapper(MAPPER, null, session, emitter, logger, Optional.ofNullable(config.getCredentialsProvider()));
      } catch (PermissionDeniedException permissionDeniedException) {
        logger.error("{} While discovering {} service", permissionDeniedException.getMessage(), service.service());
      }
    });
  }

  public List<String> getProjectList() {
    return config.getProjectListProvider().orElse(() -> {
          var projects = new ArrayList<String>();
          try (ProjectsClient projectsClient = ProjectsClient.create()) {
              projectsClient.searchProjects("").iterateAll().forEach(project -> projects.add(project.getProjectId()));
          } catch (IOException e) {
              DiscoveryExceptions.onDiscoveryException("Project::List", e);
          }
          return projects;
      }).get();
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public void init(GCPDiscoveryConfig config, Logger logger) {
//    Sentry.init();
    logger.info("In init");
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
