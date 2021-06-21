package io.openraven.magpie.core.cspm;

import io.openraven.magpie.core.cspm.services.PolicyContext;

import java.util.List;
import java.util.Map;

public class ScanResults {
  private List<PolicyContext> policies = List.of();
  private Map<PolicyContext, List<Violation>> violations = Map.of();
  private Map<PolicyContext, Map<Rule, IgnoredReason>> ignoredRules = Map.of();
  private int numOfViolations;

  public ScanResults(List<PolicyContext> policies, Map<PolicyContext, List<Violation>> violations, Map<PolicyContext, Map<Rule, IgnoredReason>> ignoredRules, int numOfViolations) {
    this.policies = policies;
    this.violations = violations;
    this.ignoredRules = ignoredRules;
    this.numOfViolations = numOfViolations;
  }

  public List<PolicyContext> getPolicies() {
    return policies;
  }

  public void setPolicies(List<PolicyContext> policies) {
    this.policies = policies;
  }

  public Map<PolicyContext, List<Violation>> getViolations() {
    return violations;
  }

  public void setViolations(Map<PolicyContext, List<Violation>> violations) {
    this.violations = violations;
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
