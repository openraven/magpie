package io.openraven.magpie.core.cspm;

import java.util.ArrayList;
import java.util.List;

public class Rule {
  private final String type = "asset";
  private String id;
  private String ruleId;
  private String ruleName;
  private String description;
  private Severity severity;
  private boolean enabled = true;
  private boolean manualControl = false;
  private String sql;
  private String eval;
  private String remediation;
  private List<String> remediationDocURLs = new ArrayList<>();
  private String version;

  public Rule() {
  }

  public Rule(String id) {
    this.id = id;
  }

  public boolean isManualControl() {
    return manualControl;
  }

  public void setManualControl(boolean manualControl) {
    this.manualControl = manualControl;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getRuleId() {
    return ruleId;
  }

  public void setRuleId(String ruleId) {
    this.ruleId = ruleId;
  }

  public String getRuleName() {
    return ruleName;
  }

  public void setRuleName(String ruleName) {
    this.ruleName = ruleName;
  }

  public String getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Severity getSeverity() {
    return severity;
  }

  public void setSeverity(Severity severity) {
    this.severity = severity;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getSql() {
    return sql;
  }

  public void setSql(String sql) {
    this.sql = sql;
  }

  public String getRemediation() {
    return remediation;
  }

  public void setRemediation(String remediation) {
    this.remediation = remediation;
  }

  public List<String> getRemediationDocURLs() {
    return remediationDocURLs;
  }

  public void setRemediationDocURLs(List<String> remediationDocURLs) {
    this.remediationDocURLs = remediationDocURLs;
  }

  public String getEval() {
    return eval;
  }

  public void setEval(String eval) {
    this.eval = eval;
  }
}
