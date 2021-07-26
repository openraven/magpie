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


import java.util.Map;

public class DMapMLRequest {
  private Metadata metadata = new Metadata();
  private Map<String, String> signature;

  public Metadata getMetadata() {
    return metadata;
  }

  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }

  public Map<String, String> getSignature() {
    return signature;
  }

  public void setSignature(Map<String, String> signature) {
    this.signature = signature;
  }

  public static class Metadata {
    private String appName = ""; // TODO remove defults
    private String appVersion = "";
    private String src = "";

    public String getAppName() {
      return appName;
    }

    public void setAppName(String appName) {
      this.appName = appName;
    }

    public String getAppVersion() {
      return appVersion;
    }

    public void setAppVersion(String appVersion) {
      this.appVersion = appVersion;
    }

    public String getSrc() {
      return src;
    }

    public void setSrc(String src) {
      this.src = src;
    }
  }
}
