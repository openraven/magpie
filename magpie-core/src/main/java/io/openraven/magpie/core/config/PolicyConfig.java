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

package io.openraven.magpie.core.config;


import java.util.List;

public class PolicyConfig {
  private String root = "~/.magpie/policies";
  private List<String> repositories = List.of();

  public String getRoot() {
    return root;
  }

  public void setRoot(String root) {
    this.root = root;
  }

  public List<String> getRepositories() {
    return repositories;
  }

  public void setRepositories(List<String> repositories) {
    this.repositories = repositories == null ? List.of() : repositories;
  }
}
