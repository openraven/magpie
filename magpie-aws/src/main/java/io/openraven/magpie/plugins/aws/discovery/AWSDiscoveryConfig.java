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

package io.openraven.magpie.plugins.aws.discovery;

import java.util.List;

public class AWSDiscoveryConfig {
  private List<String> services = List.of();
  private List<String> regions = List.of();
  private List<String> ignoreRegions = List.of();

  /**
   * @return
   * The list of AWS services to enabled for discovery.  By default this list is empty, meaning that all services will
   * be scanned.  If this list is not empty, the only the listed services will be scanned.
   */
  public List<String> getServices() {
    return services;
  }

  public void setServices(List<String> services) {
    this.services = services == null ? List.of() : services;
  }

  public List<String> getRegions() {
    return regions;
  }

  public void setRegions(List<String> regions) {
    this.regions = regions == null ? List.of() : regions;
  }

  public List<String> getIgnoreRegions() {
    return ignoreRegions;
  }

  public void setIgnoreRegions(List<String> ignoreRegions) {
    this.ignoreRegions = ignoreRegions;
  }
}
