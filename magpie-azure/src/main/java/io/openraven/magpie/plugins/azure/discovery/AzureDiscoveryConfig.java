/*
 * Copyright 2024 Open Raven Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
            return List.of();
        }
        return credentialsProvider.get();
    }
}
