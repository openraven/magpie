package io.openraven.magpie.plugins.azure.discovery;

import java.util.List;

public class AzureDiscoveryConfig {

  private List<String> services = List.of();

  public List<String> getServices() {
    return services;
  }

  public void setServices(List<String> services) {
    this.services = services == null ? List.of() : services;
  }
}
