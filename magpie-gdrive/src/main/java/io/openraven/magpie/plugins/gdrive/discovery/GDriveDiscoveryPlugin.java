package io.openraven.magpie.plugins.gdrive.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.rpc.PermissionDeniedException;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.TeamDriveList;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.OriginPlugin;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gdrive.discovery.exception.DiscoveryExceptions;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import io.openraven.magpie.plugins.gdrive.discovery.exception.GDriveDiscoveryException;
import io.openraven.magpie.plugins.gdrive.discovery.services.DriveDiscovery;
import io.openraven.magpie.plugins.gdrive.discovery.services.GDriveDiscovery;
import io.openraven.magpie.plugins.gdrive.discovery.services.SharedDriveDiscovery;
import com.google.api.services.drive.Drive;
import org.slf4j.Logger;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;

import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.DriveList;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class GDriveDiscoveryPlugin implements OriginPlugin<GDriveDiscoveryConfig> {
  public final static String ID = "magpie.gdrive.discovery";
  protected static final ObjectMapper MAPPER = GDriveUtils.createObjectMapper();
  private static final java.io.File CREDENTIALS_FOLDER = new java.io.File(System.getProperty("user.home"), "credentials");
  private static final String CLIENT_SECRET_FILE_NAME = "client_secret.json";
  private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();


  private static final List<GDriveDiscovery> PER_PROJECT_DISCOVERY_LIST = List.of(
    new SharedDriveDiscovery(),
    new DriveDiscovery());

  GDriveDiscoveryConfig config;

  private Logger logger;


  @Override
  public void discover(Session session, Emitter emitter) throws GeneralSecurityException, IOException {
    getDriveList().forEach(project -> PER_PROJECT_DISCOVERY_LIST
      .stream()
      .filter(service -> isEnabled(service.service()))
      .forEach(gDriveDiscovery -> {
        try {
          logger.debug("Discovering service: {}, class: {}", gDriveDiscovery.service(), gDriveDiscovery.getClass());
          gDriveDiscovery.discoverWrapper(MAPPER, session, emitter, logger);
        } catch (Exception ex) {
          logger.error("Discovery error in service {} - {}", gDriveDiscovery.service(), ex.getMessage());
          logger.debug("Details", ex);
        }
      }));
  }

  private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
    throws IOException {
    // Load client secrets.

    java.io.File clientSecretFilePath = new java.io.File(CREDENTIALS_FOLDER, CLIENT_SECRET_FILE_NAME);

    if (!clientSecretFilePath.exists()) {
      throw new FileNotFoundException("Please copy " + CLIENT_SECRET_FILE_NAME //
        + " to folder: " + CREDENTIALS_FOLDER.getAbsolutePath());
    }

    // Load client secrets.
    InputStream in = new FileInputStream(clientSecretFilePath);

    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

    // Build flow and trigger user authorization request.
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
      clientSecrets, SCOPES).setDataStoreFactory(new FileDataStoreFactory(CREDENTIALS_FOLDER))
      .setAccessType("offline").build();

    return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
  }

  //get drive list contained in a workspace

  public List<String> getDriveList() throws IOException, GeneralSecurityException {
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    Credential credential = getCredentials(HTTP_TRANSPORT);
    Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).build();
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

