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

package io.openraven.magpie.core.dmap.dto;

import io.openraven.magpie.core.dmap.client.dto.AppProbability;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FingerprintAnalysis {
  private String resourceId;
  private String region;
  private String address;
  private Map<Integer, List<AppProbability>> predictionsByPort = new HashMap<>(); // Default empty predictions

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  public String getResourceId() {
    return resourceId;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getAddress() {
    return address;
  }

  public Map<Integer, List<AppProbability>> getPredictionsByPort() {
    return predictionsByPort;
  }

  public void setPredictionsByPort(Map<Integer, List<AppProbability>> predictionsByPort) {
    this.predictionsByPort = predictionsByPort;
  }

  @Override
  public String toString() {
    return "FingerprintAnalysis{" +
      "resourceId='" + resourceId + '\'' +
      ", address='" + address + '\'' +
      ", predictionsByPort=" + predictionsByPort +
      '}';
  }
}
