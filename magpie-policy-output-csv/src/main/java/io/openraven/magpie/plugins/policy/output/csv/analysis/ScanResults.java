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
package io.openraven.magpie.plugins.policy.output.csv.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.openraven.magpie.plugins.policy.output.csv.model.Policy;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScanResults {
  private ScanMetadata scanMetadata;
  private List<Policy> policies;
  private List<Violation> violations;
  private List<IgnoredRule> ignoredRules;

  public ScanMetadata getScanMetadata() {
    return scanMetadata;
  }

  public void setScanMetadata(ScanMetadata scanMetadata) {
    this.scanMetadata = scanMetadata;
  }

  public List<Policy> getPolicies() {
    return policies;
  }

  public void setPolicies(List<Policy> policies) {
    this.policies = policies;
  }

  public List<Violation> getViolations() {
    return violations;
  }

  public void setViolations(List<Violation> violations) {
    this.violations = violations;
  }

  public List<IgnoredRule> getIgnoredRules() {
    return ignoredRules;
  }

  public void setIgnoredRules(List<IgnoredRule> ignoredRules) {
    this.ignoredRules = ignoredRules;
  }

  @Override
  public String toString() {
    return "ScanResults{" +
      "scanMetadata=" + scanMetadata +
      ", policies=" + policies +
      ", violations=" + violations +
      ", ignoredRules=" + ignoredRules +
      '}';
  }
}
