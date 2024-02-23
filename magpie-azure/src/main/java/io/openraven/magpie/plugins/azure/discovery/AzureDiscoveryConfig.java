package io.openraven.magpie.plugins.azure.discovery;

import com.azure.core.credential.TokenCredential;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class AzureDiscoveryConfig {

  private List<String> services = List.of();
private Supplier<List<? extends TokenCredential>> credentialsProvider = Collections::emptyList;

  public List<String> getServices() {
    return services;
  }

  public void setServices(List<String> services) {
    this.services = services == null ? List.of() : services;
  }


  public void setCredentialsProvider(Supplier<List<? extends TokenCredential>> credentials) {
      this.credentialsProvider = credentials;
  }

    public List<? extends TokenCredential> getCredentials() {
        if(credentialsProvider.get().isEmpty()) {
            return List.of(null);
        }
        return credentialsProvider.get();
    }
}
