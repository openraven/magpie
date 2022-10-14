package io.openraven.magpie.plugins.gcp.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.InstancesListResponse;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gcp.sql.SqlInstance;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;

public class SqlDiscovery implements GCPDiscovery {
  private static final String SERVICE = "sql";


  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger, Optional<CredentialsProvider> maybeCredentialsProvider) {
    final String RESOURCE_TYPE = SqlInstance.RESOURCE_TYPE;

    try {
      SQLAdmin sqlAdmin = initService(maybeCredentialsProvider);

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

  private static SQLAdmin initService(Optional<CredentialsProvider> maybeCredentialsProvider) throws GeneralSecurityException, IOException {
    GoogleCredentials credential;
    if(maybeCredentialsProvider.isPresent()){
      credential = (GoogleCredentials) maybeCredentialsProvider.get().getCredentials();
    } else {
      credential = GoogleCredentials.getApplicationDefault();
    }
    return new SQLAdmin.Builder(
      GoogleNetHttpTransport.newTrustedTransport(),
      JacksonFactory.getDefaultInstance(),
      new HttpCredentialsAdapter(credential))
      .setApplicationName("sql-instance")
      .build();
  }
}
