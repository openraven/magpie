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

public class VpcConfig {
  private String subnetId;
  private List<String> securityGroupIds;

  public VpcConfig(String subnetId, List<String> securityGroupIds) {
    this.subnetId = subnetId;
    this.securityGroupIds = securityGroupIds;
  }

  public String getSubnetId() {
    return subnetId;
  }

  public List<String> getSecurityGroupIds() {
    return securityGroupIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    VpcConfig vpcConfig = (VpcConfig) o;

    if (subnetId != null ? !subnetId.equals(vpcConfig.subnetId) : vpcConfig.subnetId != null) return false;
    return securityGroupIds != null ? securityGroupIds.equals(vpcConfig.securityGroupIds) : vpcConfig.securityGroupIds == null;
  }

  @Override
  public int hashCode() {
    int result = subnetId != null ? subnetId.hashCode() : 0;
    result = 31 * result + (securityGroupIds != null ? securityGroupIds.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "VpcConfig{" +
      "subnetId='" + subnetId + '\'' +
      ", securityGroupIds=" + securityGroupIds +
      '}';
  }
}
