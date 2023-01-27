package io.openraven.magpie.plugins.gdrive.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.gax.core.CredentialsProvider;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.DriveList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.MagpieGdriveResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gdrive.shareddrive.SharedDrive;
import io.openraven.magpie.plugins.gdrive.discovery.GDriveDiscoveryPlugin;
import io.openraven.magpie.plugins.gdrive.discovery.GDriveUtils;
import io.openraven.magpie.plugins.gdrive.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;

public class SharedDriveDiscovery implements GDriveDiscovery{

  private static final String SERVICE = "shareddrive";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();



  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, Session session, Emitter emitter, Logger logger) {
    final String RESOURCE_TYPE = SharedDrive.RESOURCE_TYPE;
    HttpRequestInitializer requestInitializer = null;
    try {
      ServiceAccountCredentials serviceAccountCredentials = ServiceAccountCredentials.fromStream(new FileInputStream("/Users/tara/credentials/oss-test.json"));
      requestInitializer = new HttpCredentialsAdapter(serviceAccountCredentials);
      final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
      Drive drive = new Drive.Builder(new NetHttpTransport(), JSON_FACTORY, requestInitializer).build();
      DriveList driveResults = drive.drives().list().execute();

    } catch (IOException IOException) {
      throw new RuntimeException("Credential File Not Found");
    } catch (GeneralSecurityException e) {
      e.printStackTrace();
    }

  }

}
