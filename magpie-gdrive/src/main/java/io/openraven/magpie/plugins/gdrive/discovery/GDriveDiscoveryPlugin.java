package io.openraven.magpie.plugins.gdrive.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.rpc.PermissionDeniedException;
import com.google.cloud.resourcemanager.v3.ProjectsClient;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.OriginPlugin;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gdrive.discovery.exception.DiscoveryExceptions;
import io.openraven.magpie.plugins.gdrive.discovery.exception.GDriveDiscoveryException;
import io.openraven.magpie.plugins.gdrive.discovery.services.DriveDiscovery;
import io.openraven.magpie.plugins.gdrive.discovery.services.GDriveDiscovery;
import io.openraven.magpie.plugins.gdrive.discovery.services.SharedDriveDiscovery;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GDriveDiscoveryPlugin implements OriginPlugin<GDriveDiscoveryConfig> {
  public final static String ID = "magpie.gdrive.discovery";
  protected static final ObjectMapper MAPPER = GDriveUtils.createObjectMapper();

  private static final List<GDriveDiscovery> PER_PROJECT_DISCOVERY_LIST = List.of(
    new SharedDriveDiscovery(),
    new DriveDiscovery());

  private static final List<GDriveDiscovery> SINGLE_DISCOVERY_LIST = List.of(
    new ResourceManagerDiscovery());

  GDriveDiscovery config;

  private Logger logger;


  @Override
  public void discover(Session session, Emitter emitter) {
    getProjectList().forEach(project -> PER_PROJECT_DISCOVERY_LIST
      .stream()
      .filter(service -> isEnabled(service.service()))
      .forEach(gDriveDiscovery -> {
        try {
          logger.debug("Discovering service: {}, class: {}", gDriveDiscovery.service(), gDriveDiscovery.getClass());
          gDriveDiscovery.discoverWrapper(MAPPER, project, session, emitter, logger, Optional.ofNullable(config.getCredentialsProvider()));
        } catch (Exception ex) {
          logger.error("Discovery error in service {} - {}", gDriveDiscovery.service(), ex.getMessage());
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
  public void init(GDriveDiscoveryConfig config, Logger logger) {
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
  public Class<GDriveDiscoveryConfig> configType() {
    return GDriveDiscoveryConfig.class;
  }
}

