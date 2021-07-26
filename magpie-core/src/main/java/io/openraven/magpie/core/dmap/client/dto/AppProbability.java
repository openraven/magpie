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

package io.openraven.magpie.core.dmap.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class AppProbability implements Comparable<AppProbability> {

  @JsonAlias("app_name")
  private String appName;

  private double probability;

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public double getProbability() {
    return probability;
  }

  public void setProbability(double probability) {
    this.probability = probability;
  }

  @Override
  public int compareTo(AppProbability other) {
    if (this.probability > other.probability) {
      return 1;
    } else if (this.probability < other.probability) {
      return -1;
    }
    return 0;
  }

  @Override
  public String toString() {
    return "AppProbability{" +
      "appName='" + appName + '\'' +
      ", probability=" + probability +
      '}';
  }
}
