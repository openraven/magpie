package io.openraven.magpie.core.cspm.services;

import io.openraven.magpie.core.cspm.Policy;

public class PolicyContext {
  private final PolicyMetadata metadata;
  private final Policy policy;

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
