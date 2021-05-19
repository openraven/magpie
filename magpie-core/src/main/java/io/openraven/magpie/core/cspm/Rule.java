package io.openraven.magpie.core.cspm;

import java.util.ArrayList;
import java.util.List;

public class Rule {
  private final String type = "asset";
  private String id;
  private String refId;
  private String name;
  private String description;
  private Severity severity;
  private boolean enabled = true;
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

  public void set(Rule yamlRule) {
    this.severity = yamlRule.getSeverity();
    this.name = yamlRule.getName();
    this.remediationDocURLs = yamlRule.getRemediationDocURLs();
    this.description = yamlRule.getDescription();
    this.enabled = yamlRule.isEnabled();
    this.eval = yamlRule.getEval();
    this.enabled = yamlRule.isEnabled();
    this.id = yamlRule.getId();
    this.refId = yamlRule.getRefId();
    this.sql = yamlRule.getSql();
    this.version = yamlRule.getVersion();
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

  public String getRefId() {
    return refId;
  }

  public void setRefId(String refId) {
    this.refId = refId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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
