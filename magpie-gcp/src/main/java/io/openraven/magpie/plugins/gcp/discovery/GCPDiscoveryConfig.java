/*
 * Copyright 2021 Open Raven Inc
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

package io.openraven.magpie.plugins.gcp.discovery;

import com.google.api.gax.core.CredentialsProvider;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class GCPDiscoveryConfig {
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
