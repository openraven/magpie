package io.openraven.magpie.core.cspm;

import io.openraven.magpie.core.cspm.services.PolicyContext;

import java.util.List;
import java.util.Map;

public class ScanResults {

  public enum UnscannedReason {
    DISABLED("Disabled via configuration"),
    MISSING_ASSET("Asset table not found"),
    MANUAL_CONTROL("Manual Control");

    private final String reason;

    UnscannedReason(String reason) {
      this.reason = reason;
    }

    public String getReason() {
      return reason;
    }
  }


  private List<PolicyContext> policies = List.of();
  private List<Violation> violations = List.of();
  private Map<Rule, UnscannedReason> unscannedRules = Map.of();

  public Map<Rule, UnscannedReason> getUnscannedRules() {
    return unscannedRules;
  }

  public void setUnscannedRules(Map<Rule, UnscannedReason> unscannedRules) {
    this.unscannedRules = unscannedRules;
  }

  public List<PolicyContext> getPolicies() {
    return policies;
  }

  public void setPolicies(List<PolicyContext> policies) {
    this.policies = policies;
  }

  public List<Violation> getViolations() {
    return violations;
  }

  public void setViolations(List<Violation> violations) {
    this.violations = violations;
  }
}
