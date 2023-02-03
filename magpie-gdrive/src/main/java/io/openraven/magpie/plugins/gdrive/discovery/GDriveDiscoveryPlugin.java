package io.openraven.magpie.plugins.gdrive.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.http.HttpTransportFactory;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.OriginPlugin;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gdrive.discovery.services.GDriveDiscovery;
import io.openraven.magpie.plugins.gdrive.discovery.services.SharedDriveDiscovery;
import com.google.api.services.drive.Drive;
import org.slf4j.Logger;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;

import com.google.api.services.drive.model.DriveList;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;

public class GDriveDiscoveryPlugin implements OriginPlugin<GDriveDiscoveryConfig> {
  public final static String ID = "magpie.gdrive.discovery";
  protected static final ObjectMapper MAPPER = GDriveUtils.createObjectMapper();
  public static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  public static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_READONLY);



  private static final List<GDriveDiscovery> PER_PROJECT_DISCOVERY_LIST = List.of(
    new SharedDriveDiscovery());

  GDriveDiscoveryConfig config;

  private Logger logger;


  @Override
  public void discover(Session session, Emitter emitter) throws GeneralSecurityException, IOException {
    getDriveList().forEach(projectId -> PER_PROJECT_DISCOVERY_LIST
      .stream()
      .filter(service -> isEnabled(service.service()))
      .forEach(gDriveDiscovery -> {
        try {
          logger.debug("Discovering service: {}, class: {}", gDriveDiscovery.service(), gDriveDiscovery.getClass());
          gDriveDiscovery.discoverWrapper(MAPPER, session, emitter, logger, projectId);
        } catch (Exception ex) {
          logger.error("Discovery error in service {} - {}", gDriveDiscovery.service(), ex.getMessage());
          logger.debug("Details", ex);
        }
      }));
  }

  //get drive list contained in a workspace

  public List<String> getDriveList() throws IOException, GeneralSecurityException {

    GoogleCredentials credentials = GoogleCredentials.getApplicationDefault().createScoped(SCOPES);
    HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer).build();
    DriveList driveResults = service.drives().list().execute();
    return config.getDriveListProvider().orElse(() -> {
      var drives = new ArrayList<String>();
      List<com.google.api.services.drive.model.Drive> drive = driveResults.getDrives();
      for (com.google.api.services.drive.model.Drive driveItem : drive){
        drives.add(driveItem.getId());
      }
      return drives;
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

