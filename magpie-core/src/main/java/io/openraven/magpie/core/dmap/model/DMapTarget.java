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

package io.openraven.magpie.core.dmap.model;

import java.util.List;

public class DMapTarget {

  private String resourceId;
  private String region;
  private String subnetId;
  private String privateIpAddress;
  private List<String> securityGroups;

  public DMapTarget(String resourceId,
                    String region,
                    String subnetId,
                    String privateIpAddress,
                    List<String> securityGroups) {
    this.resourceId = resourceId;
    this.region = region;
    this.subnetId = subnetId;
    this.privateIpAddress = privateIpAddress;
    this.securityGroups = securityGroups;
  }

  public String getResourceId() {
    return resourceId;
  }

  public String getRegion() {
    return region;
  }

  public String getSubnetId() {
    return subnetId;
  }

  public String getPrivateIpAddress() {
    return privateIpAddress;
  }

  public List<String> getSecurityGroups() {
    return securityGroups;
  }
}
