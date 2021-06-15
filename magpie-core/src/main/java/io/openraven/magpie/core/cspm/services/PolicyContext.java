package io.openraven.magpie.core.cspm.services;

import io.openraven.magpie.core.cspm.Policy;
import io.openraven.magpie.core.cspm.Rule;

import java.util.Map;

public class PolicyContext {

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

  private final PolicyMetadata metadata;
  private final Policy policy;
  private Map<Rule, IgnoredReason> ignored = Map.of();

  public Map<Rule, IgnoredReason> getIgnored() {
    return ignored;
  }

  public void setIgnored(Map<Rule, IgnoredReason> ignoredReason) {
    this.ignored = ignoredReason;
  }

  public PolicyContext(PolicyMetadata metadata, Policy policy) {
    this.metadata = metadata;
    this.policy = policy;
  }

  public PolicyMetadata getMetadata() {
    return metadata;
  }

  public Policy getPolicy() {
    return policy;
  }
}
