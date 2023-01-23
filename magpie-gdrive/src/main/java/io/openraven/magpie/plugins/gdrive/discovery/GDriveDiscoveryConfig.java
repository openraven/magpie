package io.openraven.magpie.plugins.gdrive.discovery;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import java.util.Collections;
import java.util.List;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.util.Optional;
import java.util.function.Supplier;

public class GDriveDiscoveryConfig {
  private List<String> drives = List.of();
  private List<String> services = List.of();
  private Optional<Supplier<List<String>>> driveListProvider = Optional.empty();
  private CredentialsProvider credentialsProvider;




  public List<String> getDrives() {return drives;}

  public List<String> getServices() {
    return services;
  }

  public CredentialsProvider getCredentialsProvider() {
    return credentialsProvider;
  }

  public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
    this.credentialsProvider = credentialsProvider;
  }

  public void setServices(List<String> services) {
    this.services = services == null ? List.of() : services;
  }

  public Optional<Supplier<List<String>>> getDriveListProvider() {
    return this.driveListProvider;
  }

  public void setDriveListProvider(Supplier<List<String>> driveListProvider) {
    this.driveListProvider = Optional.ofNullable(driveListProvider);
  }
}
