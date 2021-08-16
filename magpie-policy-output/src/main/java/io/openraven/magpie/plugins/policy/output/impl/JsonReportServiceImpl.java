package io.openraven.magpie.plugins.policy.output.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openraven.magpie.api.PolicyOutputPlugin;
import io.openraven.magpie.api.cspm.PolicyContext;
import io.openraven.magpie.api.cspm.Rule;
import io.openraven.magpie.api.cspm.ScanResults;
import io.openraven.magpie.api.cspm.Violation;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class JsonReportServiceImpl implements PolicyOutputPlugin<Void> {

  private static final String ID = "magpie.policy.output.json";

  private static final ObjectMapper MAPPER = new ObjectMapper()
    .enable(SerializationFeature.INDENT_OUTPUT)
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .findAndRegisterModules();

  @Override
  public void generateReport(ScanResults results) {

    try {
      ObjectNode parentNode = MAPPER.createObjectNode();

      generateReportMeta(results, parentNode);
      generateDisabledPolicies(results, parentNode);
      generatePolicyAnalysis(results, parentNode);

      MAPPER.writeValue(System.out, parentNode);
    } catch (IOException e) {
      throw new RuntimeException("Unable to serialize policy analysis results to JSON format", e);
    }
  }

  private void generatePolicyAnalysis(ScanResults results, ObjectNode parentNode) {
    ArrayNode policiesArrayNode = MAPPER.createArrayNode();
    parentNode.set("policies", policiesArrayNode);
    results.getPolicies().forEach(policy -> {

      var policyViolations = results.getViolations().get(policy);
      var ignoredRules = results.getIgnoredRules().get(policy);

      ObjectNode policyNode = MAPPER.createObjectNode();
      policyNode.put("name", policy.getPolicy().getPolicyName());
      policyNode.put("violationsCount", policyViolations == null ? 0 : policyViolations.size());

      generateViolation(policy, policyViolations, policyNode);
      generateIgnoredRule(ignoredRules, policyNode);

      policiesArrayNode.add(policyNode);

    });
  }

  private void generateIgnoredRule(java.util.Map<Rule, ScanResults.IgnoredReason> ignoredRules, ObjectNode policyNode) {
    if (ignoredRules != null) {
      ArrayNode ignoredRulesArray = MAPPER.createArrayNode();
      policyNode.set("ignoredRules", ignoredRulesArray);

      ignoredRules.forEach((rule, reason) -> {
        ObjectNode ignoredRuleNode = MAPPER.createObjectNode();
        ignoredRuleNode.put("name", rule.getRuleName());
        ignoredRuleNode.put("file", rule.getFileName());
        ignoredRuleNode.put("reason", reason.getReason());
        ignoredRulesArray.add(ignoredRuleNode);
      });
    }
  }

  private void generateViolation(PolicyContext policy, List<Violation> policyViolations, ObjectNode policyNode) {
    ArrayNode violationsArray = MAPPER.createArrayNode();
    policyNode.set("violations", violationsArray);

    if (policyViolations != null) {
      policyViolations.forEach(policyViolation -> {

        Rule violatedRule = policy.getPolicy().getRules().stream().filter(rule -> rule.getId().equals(policyViolation.getRuleId())).findFirst().get();

        ObjectNode violatedRuleNode = MAPPER.createObjectNode();
        violatedRuleNode.put("name", violatedRule.getRuleName());
        violatedRuleNode.put("file", violatedRule.getFileName());
        violatedRuleNode.put("severety", violatedRule.getSeverity().getTitle());

        ObjectNode violationNode = MAPPER.createObjectNode();
        violationNode.put("resourceID", policyViolation.getAssetId());
        violationNode.set("violatedRule", violatedRuleNode);

        violationsArray.add(violationNode);

      });
    }
  }

  private void generateDisabledPolicies(ScanResults results, ObjectNode parentNode) {
    ArrayNode disabledPoliciesNode = MAPPER.createArrayNode();
    parentNode.set("disabledPolicies", disabledPoliciesNode);
    var disabledPolicies = results.getPolicies().stream().filter(policy -> !policy.getPolicy().isEnabled()).collect(Collectors.toList());
    if (!disabledPolicies.isEmpty()) {
      disabledPolicies.forEach(policy -> {
        ObjectNode policyNode = MAPPER.createObjectNode();
        policyNode.put("Policy GUID", policy.getPolicy().getId());
        policyNode.put("Policy name", policy.getPolicy().getPolicyName());
        disabledPoliciesNode.add(policyNode);
      });
    }
  }

  private void generateReportMeta(ScanResults results, ObjectNode parentNode) {
    ObjectNode metaNode = MAPPER.createObjectNode();
    metaNode.put("startTime", results.getScanMetadata().getStartDateTime().toString());
    metaNode.put("duration", humanReadableFormat(results.getScanMetadata().getDuration()));
    parentNode.set("meta", metaNode);
  }

  private String humanReadableFormat(Duration duration) {
    return duration.toString()
      .substring(2)
      .replaceAll("(\\d[HMS])(?!$)", "$1 ")
      .toLowerCase();
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public void init(Void unused, Logger logger) {
    // Nothing here yet
  }

  @Override
  public Class<Void> configType() {
    return null;
  }
}
