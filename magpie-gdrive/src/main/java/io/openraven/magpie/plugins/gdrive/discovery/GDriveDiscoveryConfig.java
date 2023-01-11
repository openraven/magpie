package io.openraven.magpie.plugins.gdrive.discovery;

import com.google.api.gax.core.CredentialsProvider;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class GDriveDiscoveryConfig {
  private List<String> services = List.of();

  private CredentialsProvider credentialsProvider;
  private Optional<Supplier<List<String>>> projectListProvider = Optional.empty();

  public List<String> getServices() {
    return services;
  }

  public void setServices(List<String> services) {
    this.services = services == null ? List.of() : services;
  }

  public CredentialsProvider getCredentialsProvider() {
    return credentialsProvider;
  }

  public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
    this.credentialsProvider = credentialsProvider;
  }

  public Optional<Supplier<List<String>>> getProjectListProvider() {
    return this.projectListProvider;
  }

  public void setProjectListProvider(Supplier<List<String>> projectListProvider) {
    this.projectListProvider = Optional.ofNullable(projectListProvider);
  }
}
