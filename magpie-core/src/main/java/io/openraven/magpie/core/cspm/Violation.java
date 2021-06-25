package io.openraven.magpie.core.cspm;

import java.time.LocalDateTime;

public class Violation {
  private String policyId;
  private String ruleId;
  private String assetId;
  private String info;
  private String error;
  private LocalDateTime evaluatedAt;

  public Violation() {
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getInfo() {
    return info;
  }

  public void setInfo(String info) {
    this.info = info;
  }

  public LocalDateTime getEvaluatedAt() {
    return evaluatedAt;
  }

  public void setEvaluatedAt(LocalDateTime evaluatedAt) {
    this.evaluatedAt = evaluatedAt;
  }

  public String getPolicyId() {
    return policyId;
  }

  public void setPolicyId(String policyId) {
    this.policyId = policyId;
  }

  public String getAssetId() {
    return assetId;
  }

  public void setAssetId(String arn) {
    this.assetId = arn;
  }

  public String getRuleId() {
    return ruleId;
  }

  public void setRuleId(String ruleId) {
    this.ruleId = ruleId;
  }

  @Override
  public String toString() {
    return "Violation{" +
            "policyId='" + policyId + '\'' +
            ", ruleId='" + ruleId + '\'' +
            ", assetId='" + assetId + '\'' +
            ", info='" + info + '\'' +
            ", error='" + error + '\'' +
            ", evaluatedAt=" + evaluatedAt +
            '}';
  }

}
