package io.openraven.magpie.plugins.gcp.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.compute.v1.Project;
import com.google.cloud.compute.v1.ProjectClient;
import com.google.cloud.compute.v1.ProjectSettings;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gcp.project.ProjectInfo;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

public class ProjectDiscovery implements GCPDiscovery {
  private static final String SERVICE = "project";


  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger, Optional<CredentialsProvider> maybeCredentialsProvider) {
    final String RESOURCE_TYPE = ProjectInfo.RESOURCE_TYPE;
    var builder = ProjectSettings.newBuilder();
    maybeCredentialsProvider.ifPresent(builder::setCredentialsProvider);
    try (ProjectClient projectClient = ProjectClient.create(builder.build())) {
      Project project = projectClient.getProject(projectId);

      String assetId = "project::%s";
      var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, String.format(assetId, project.getName()))
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withConfiguration(GCPUtils.asJsonNode(project))
        .build();

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":project"), data.toJsonNode()));
    } catch (Exception e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }
}
