package io.openraven.magpie.core.cspm.services;

public class PolicyMetadata {

  private final String policyPath;
  private final String repoHash;

  public PolicyMetadata(String policyPath, String repoHash) {
    this.policyPath = policyPath;
    this.repoHash = repoHash;
  }

  public String getPolicyPath() {
    return policyPath;
  }

  public String getRepoHash() {
    return repoHash;
  }
}
