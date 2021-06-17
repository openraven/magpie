package io.openraven.magpie.core.cspm;

import io.openraven.magpie.core.cspm.services.PolicyContext;

import java.util.List;
import java.util.Map;

public class ScanResults {
  private Map<PolicyContext, List<Violation>> violations = Map.of();
  private Map<PolicyContext, IgnoredReason> ignoredPolicies = Map.of();
  private Map<PolicyContext, Map<Rule, IgnoredReason>> ignoredRules = Map.of();
  private int numOfViolations;

  public ScanResults(Map<PolicyContext, List<Violation>> violations, Map<PolicyContext, IgnoredReason> ignoredPolicies, Map<PolicyContext, Map<Rule, IgnoredReason>> ignoredRules, int numOfViolations) {
    this.violations = violations;
    this.ignoredPolicies = ignoredPolicies;
    this.ignoredRules = ignoredRules;
    this.numOfViolations = numOfViolations;
  }

  public Map<PolicyContext, Map<Rule, IgnoredReason>> getIgnoredRules() {
    return ignoredRules;
  }

  public void setIgnoredRules(Map<PolicyContext, Map<Rule, IgnoredReason>> ignoredRules) {
    this.ignoredRules = ignoredRules;
  }

  public int getNumOfViolations() {
    return numOfViolations;
  }

  public void setNumOfViolations(int numOfViolations) {
    this.numOfViolations = numOfViolations;
  }

  public Map<PolicyContext, List<Violation>> getViolations() {
    return violations;
  }

  public void setViolations(Map<PolicyContext, List<Violation>> violations) {
    this.violations = violations;
  }

  public Map<PolicyContext, IgnoredReason> getIgnoredPolicies() {
    return ignoredPolicies;
  }

  public void setIgnoredPolicies(Map<PolicyContext, IgnoredReason> ignoredPolicies) {
    this.ignoredPolicies = ignoredPolicies;
  }

  public enum IgnoredReason {
    DISABLED("Disabled via configuration"),
    MISSING_ASSET("Asset table not found"),
    MANUAL_CONTROL("Manual Control");

    private final String reason;

    IgnoredReason(String reason) {
      this.reason = reason;
    }

    public String getReason() {
      return reason;
    }
  }

}
