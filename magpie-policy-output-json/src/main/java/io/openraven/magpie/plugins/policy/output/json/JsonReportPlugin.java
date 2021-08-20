/*
 * Copyright 2021 Open Raven Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openraven.magpie.plugins.policy.output.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openraven.magpie.api.PolicyOutputPlugin;
import io.openraven.magpie.plugins.policy.output.json.analysis.IgnoredRule;
import io.openraven.magpie.plugins.policy.output.json.analysis.ScanResults;
import io.openraven.magpie.plugins.policy.output.json.analysis.Violation;
import io.openraven.magpie.plugins.policy.output.json.model.Rule;
import io.openraven.magpie.plugins.policy.output.json.model.Policy;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

public class JsonReportPlugin implements PolicyOutputPlugin<Void> {

  private static final String ID = "magpie.policy.output.json";

  private static final ObjectMapper MAPPER = new ObjectMapper()
    .enable(SerializationFeature.INDENT_OUTPUT)
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
    .findAndRegisterModules();

  @Override
  public void generateReport(ObjectNode data) {

    ScanResults results = parseData(data);

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

    Map<Policy, List<Violation>> violationMap = results.getViolations().stream()
      .collect(groupingBy(Violation::getPolicy));

    Map<Policy, List<IgnoredRule>> ignoredRulesMap = results.getIgnoredRules().stream()
      .collect(groupingBy(IgnoredRule::getPolicy));

    results.getPolicies().forEach(policy -> {

      var policyViolations = violationMap.get(policy);
      var ignoredRules = ignoredRulesMap.get(policy);

      ObjectNode policyNode = MAPPER.createObjectNode();
      policyNode.put("name", policy.getPolicyName());
      policyNode.put("violationsCount", policyViolations == null ? 0 : policyViolations.size());

      generateViolation(policy, policyViolations, policyNode);
      generateIgnoredRule(ignoredRules, policyNode);

      policiesArrayNode.add(policyNode);

    });
  }

  private void generateIgnoredRule(List<IgnoredRule> ignoredRules, ObjectNode policyNode) {
    if (ignoredRules != null) {
      ArrayNode ignoredRulesArray = MAPPER.createArrayNode();
      policyNode.set("ignoredRules", ignoredRulesArray);

      ignoredRules.forEach(rule -> {
        ObjectNode ignoredRuleNode = MAPPER.createObjectNode();
        ignoredRuleNode.put("name", rule.getRule().getRuleName());
        ignoredRuleNode.put("file", rule.getRule().getFileName());
        ignoredRuleNode.put("reason", rule.getIgnoredReason().getReason());
        ignoredRulesArray.add(ignoredRuleNode);
      });
    }
  }

  private void generateViolation(Policy policy, List<Violation> policyViolations, ObjectNode policyNode) {
    ArrayNode violationsArray = MAPPER.createArrayNode();
    policyNode.set("violations", violationsArray);

    if (policyViolations != null) {
      policyViolations.forEach(policyViolation -> {
        Rule violatedRule = policyViolation.getRule();

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
    var disabledPolicies = results.getPolicies().stream().filter(policy -> !policy.isEnabled()).collect(Collectors.toList());
    if (!disabledPolicies.isEmpty()) {
      disabledPolicies.forEach(policy -> {
        ObjectNode policyNode = MAPPER.createObjectNode();
        policyNode.put("Policy GUID", policy.getId());
        policyNode.put("Policy name", policy.getPolicyName());
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

  private ScanResults parseData(ObjectNode data) {
    try {
      return MAPPER.treeToValue(data, ScanResults.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Unable to parse data for plugin: " + id(), e);
    }
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public void init(Void config, Logger logger) {

  }

  @Override
  public void shutdown() {
    PolicyOutputPlugin.super.shutdown();
  }

  @Override
  public Class<Void> configType() {
    return null;
  }

}
