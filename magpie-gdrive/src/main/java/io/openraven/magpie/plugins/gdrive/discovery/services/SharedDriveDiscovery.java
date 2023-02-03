package io.openraven.magpie.plugins.gdrive.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.gax.core.CredentialsProvider;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.DriveList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.MagpieGdriveResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gdrive.shareddrive.SharedDrive;
import io.openraven.magpie.plugins.gdrive.discovery.GDriveDiscoveryPlugin;
import io.openraven.magpie.plugins.gdrive.discovery.GDriveUtils;
import io.openraven.magpie.plugins.gdrive.discovery.VersionedMagpieEnvelopeProvider;
import io.openraven.magpie.plugins.gdrive.discovery.exception.DiscoveryExceptions;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

public class SharedDriveDiscovery implements GDriveDiscovery{

  private static final String SERVICE = "shareddrive";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  public static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_READONLY);




  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, Session session, Emitter emitter, Logger logger, String projectId) throws IOException, GeneralSecurityException {
    final String RESOURCE_TYPE = SharedDrive.RESOURCE_TYPE;

      GoogleCredentials credentials = GoogleCredentials.getApplicationDefault().createScoped(SCOPES);
      HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);
      final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
      Drive drive = new Drive.Builder(new NetHttpTransport(), JSON_FACTORY, requestInitializer).setApplicationName(projectId).build();
      DriveList driveResults = drive.drives().list().execute();
      List<com.google.api.services.drive.model.Drive> drivers = driveResults.getDrives();
      for (com.google.api.services.drive.model.Drive driveList : drivers) {
        var data = new MagpieGdriveResource.MagpieGdriveResourceBuilder(mapper, driveList.getId())
          .withProjectId(projectId)
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(GDriveUtils.asJsonNode(driveList))
          .build();

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":shareddrive"), data.toJsonNode()));
      }
    }

  }


