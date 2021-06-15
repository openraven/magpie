package io.openraven.magpie.core.cspm;

import io.openraven.magpie.core.cspm.services.PolicyContext;

import java.util.List;

public class ScanResults {

  private List<PolicyContext> policies = List.of();
  private List<Violation> violations = List.of();

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
