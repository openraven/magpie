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

package io.openraven.magpie.core.cspm;

import java.util.List;

public class ScanTarget {

  private String resourceId;
  private String subnetId;
  private List<String> securityGroups;

  public ScanTarget(String resourceId, String subnetId, List<String> securityGroups) {
    this.resourceId = resourceId;
    this.subnetId = subnetId;
    this.securityGroups = securityGroups;
  }

  public String getResourceId() {
    return resourceId;
  }

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  public String getSubnetId() {
    return subnetId;
  }

  public void setSubnetId(String subnetId) {
    this.subnetId = subnetId;
  }

  public List<String> getSecurityGroups() {
    return securityGroups;
  }

  public void setSecurityGroups(List<String> securityGroups) {
    this.securityGroups = securityGroups;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ScanTarget that = (ScanTarget) o;

    if (!resourceId.equals(that.resourceId)) return false;
    if (!subnetId.equals(that.subnetId)) return false;
    return securityGroups.equals(that.securityGroups);
  }

  @Override
  public int hashCode() {
    int result = resourceId.hashCode();
    result = 31 * result + subnetId.hashCode();
    result = 31 * result + securityGroups.hashCode();
    return result;
  }
}
