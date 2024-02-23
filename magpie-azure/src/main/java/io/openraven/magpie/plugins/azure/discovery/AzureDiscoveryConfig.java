package io.openraven.magpie.plugins.azure.discovery;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class AzureDiscoveryConfig {

  private List<String> services = List.of();
private Supplier<List<Map<String,Object>>> credentialsProvider = Collections::emptyList;

  public List<String> getServices() {
    return services;
  }

  public void setServices(List<String> services) {
    this.services = services == null ? List.of() : services;
  }


  public void setCredentialsProvider(Supplier<List<Map<String,Object>>> credentials) {
      this.credentialsProvider = credentials;
  }

    public List<Map<String, Object>> getCredentials() {
        if(credentialsProvider.get().isEmpty()) {
            return List.of(null);
        }
        return credentialsProvider.get();
    }
}
