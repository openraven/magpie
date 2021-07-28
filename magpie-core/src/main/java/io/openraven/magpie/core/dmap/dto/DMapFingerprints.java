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

import java.util.Map;

public class DMapFingerprints {
  private String id;
  private String address;
  private Map<Integer, Map<String, String>> signatures;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public Map<Integer, Map<String, String>> getSignatures() {
    return signatures;
  }

  public void setSignatures(Map<Integer, Map<String, String>> signatures) {
    this.signatures = signatures;
  }

  @Override
  public String toString() {
    return "DMapFingerprints{" +
      "id='" + id + '\'' +
      ", address='" + address + '\'' +
      ", signatures=" + signatures +
      '}';
  }
}
