package io.openraven.magpie.plugins.gcp.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.resourcemanager.v3.Project;
import com.google.cloud.resourcemanager.v3.ProjectName;
import com.google.cloud.resourcemanager.v3.ProjectsClient;
import com.google.cloud.resourcemanager.v3.ProjectsSettings;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gcp.project.ProjectInfo;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class ProjectDiscovery implements GCPDiscovery {
  private static final String SERVICE = "project";
  private static final Logger LOGGER = LoggerFactory.getLogger(ProjectDiscovery.class);
  public static final String ASSET_ID_FORMAT = "//cloudresourcemanager.googleapis.com/%s";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger, Optional<CredentialsProvider> maybeCredentialsProvider) {
    final String RESOURCE_TYPE = ProjectInfo.RESOURCE_TYPE;
    var builder = ProjectsSettings.newBuilder();
    maybeCredentialsProvider.ifPresent(builder::setCredentialsProvider);
    try (ProjectsClient projectClient = ProjectsClient.create(builder.build())) {
      // We have to use search because this API does not offer GET by Project ID, only by project name
      String queryFormat = String.format("id:%s", projectId);
      ProjectsClient.SearchProjectsPagedResponse resp = projectClient.searchProjects(queryFormat);
      StreamSupport.stream(resp.iterateAll().spliterator(), false).filter(x -> projectId.equals(x.getProjectId()))
        .findFirst()
        .ifPresentOrElse(
          (project) -> {
            String assetId = String.format(ASSET_ID_FORMAT, ProjectName.format(projectId));
            var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, assetId)
              .withProjectId(projectId)
              .withResourceId(projectId)
              .withResourceName(project.getDisplayName())
              .withResourceType(RESOURCE_TYPE)
              .withConfiguration(GCPUtils.asJsonNode(new Configuration(project)))
              .build();

            emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":project"), data.toJsonNode()));
          },
          () -> LOGGER.error("Expected project id {} to exist, but could not find it", projectId)
        );
    } catch (Exception e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  // Based off of https://cloud.google.com/resource-manager/reference/rest/v3/projects#Project
  // We need this because the model from GCP is not suitable for serialization
  public static class Configuration {
    public String name;
    public String parent;
    public String projectId;
    public Project.State state;
    public String displayName;
    public Instant createTime;
    public Instant updateTime;
    public Instant deleteTime;
    public String etag;
    public Map<String, String> labels;

    public Configuration() {}

    public Configuration(Project project) {
      this.name = project.getName();
      this.parent = project.getParent();
      this.projectId = project.getProjectId();
      this.state = project.getState();
      this.displayName = project.getDisplayName();
      this.createTime = GCPUtils.protobufTimestampToInstant(project.getCreateTime());
      this.updateTime = GCPUtils.protobufTimestampToInstant(project.getUpdateTime());
      this.deleteTime = GCPUtils.protobufTimestampToInstant(project.getDeleteTime());
      this.etag = project.getEtag();
      this.labels = project.getLabelsMap();
    }
  }
}
