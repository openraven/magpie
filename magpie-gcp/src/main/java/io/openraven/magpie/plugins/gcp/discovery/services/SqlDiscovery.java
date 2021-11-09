package io.openraven.magpie.plugins.gcp.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.iam.v1.IamScopes;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.InstancesListResponse;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class SqlDiscovery implements GCPDiscovery {
  private static final String SERVICE = "sql";


  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger) {
    final String RESOURCE_TYPE = "GCP::SQL::Instance";

    try {
      SQLAdmin sqlAdmin = initService();

      var request = sqlAdmin.instances().list(projectId);

      InstancesListResponse response;
      do {
        response = request.execute();
        if (response.getItems() == null) {
          continue;
        }
        for (var sqlInstance : response.getItems()) {
          var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, sqlInstance.getName())
            .withProjectId(projectId)
            .withResourceType(RESOURCE_TYPE)
            .withRegion(sqlInstance.getRegion())
            .withConfiguration(GCPUtils.asJsonNode(sqlInstance))
            .build();

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":sqlInstance"), data.toJsonNode()));
        }
        request.setPageToken(response.getNextPageToken());
      } while (response.getNextPageToken() != null);

    } catch (GeneralSecurityException | IOException e) {
      logger.error("Unable to finish SQL discovery, due to: {}", e.getMessage());
    }
  }

  private static SQLAdmin initService() throws GeneralSecurityException, IOException {
    GoogleCredentials credential =
      GoogleCredentials.getApplicationDefault()
        .createScoped(Collections.singleton(IamScopes.CLOUD_PLATFORM));

    return new SQLAdmin.Builder(
      GoogleNetHttpTransport.newTrustedTransport(),
      JacksonFactory.getDefaultInstance(),
      new HttpCredentialsAdapter(credential))
      .setApplicationName("sql-instance")
      .build();
  }
}
