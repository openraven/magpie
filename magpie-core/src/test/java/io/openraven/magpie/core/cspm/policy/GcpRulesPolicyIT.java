package io.openraven.magpie.core.cspm.policy;

import io.openraven.magpie.core.cspm.analysis.IgnoredRule;
import io.openraven.magpie.core.cspm.analysis.IgnoredRule.IgnoredReason;
import io.openraven.magpie.core.cspm.analysis.ScanResults;
import io.openraven.magpie.core.cspm.analysis.Violation;
import io.openraven.magpie.core.cspm.model.PolicyContext;
import io.openraven.magpie.core.cspm.model.Rule;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static io.openraven.magpie.core.cspm.analysis.IgnoredRule.IgnoredReason.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GcpRulesPolicyIT extends AbstractPolicyIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(GcpRulesPolicyIT.class);

  private static final String GCP_ASSETS_PATH = "/json/gcp-assets.json";
  private static final String GCP_RULES_ASSETS_PATH = "/assets/gcp-rules-assets.yaml";
  private static final String GCP_POLICY_ID = "opnrvn-p-2";

  @Test
  public void testGcpRules() throws Exception {

    populateAssetData(GCP_ASSETS_PATH);

    PolicyContext gcpPolicyContext = policyMap.get(GCP_POLICY_ID);
    ScanResults scanResults = analyzePolicies(List.of(gcpPolicyContext));

    // Assert ignored rules
    List<IgnoredRule> ignoredRules = scanResults.getIgnoredRules();
    assertEquals(15, getIgnoredRulesByType(ignoredRules, MANUAL_CONTROL).size());
    assertEquals(0, getIgnoredRulesByType(ignoredRules, DISABLED).size());
    assertEquals(0, getIgnoredRulesByType(ignoredRules, MISSING_ASSET).size());

    // Assert violations
    List<Violation> violations = scanResults.getViolations();
    assertEquals(70, violations.size());

    Map<String, String> violatedAssetsPerRule = getViolatedAssetsByRule(violations);
    Map<String, String> rulesTestAssets = getRulesTestAssets(GCP_RULES_ASSETS_PATH);
    assertEquals(violatedAssetsPerRule, rulesTestAssets);

    assertEquals(50, violatedAssetsPerRule.size());
    assertEquals(15, getMissedViolations(gcpPolicyContext, violatedAssetsPerRule).size()); // <-- Not testing

  }

  private Set<String> getIgnoredRulesByType(List<IgnoredRule> ignoredRules, IgnoredReason reason) {
    return ignoredRules
      .stream()
      .filter(ignoredRule -> reason.equals(ignoredRule.getIgnoredReason()))
      .map(ignoredRule -> ignoredRule.getRule().getFileName())
      .collect(Collectors.toSet());
  }

  private Set<String> getMissedViolations(PolicyContext gcpPolicyContext, Map<String, String> violatedAssetsPerRule) {
    return gcpPolicyContext.getPolicy().getRules()
      .stream()
      .filter(rule -> !rule.isManualControl())
      .filter(Rule::isEnabled)
      .map(Rule::getRuleId)
      .filter(ruleId -> Objects.isNull(violatedAssetsPerRule.get(ruleId)))
      .peek(ruleId -> LOGGER.info("Missing violation. RuleId: {}", ruleId))
      .collect(Collectors.toSet());
  }

  private Map<String, String> getViolatedAssetsByRule(List<Violation> violations) {
    Map<String, String> violatedAssetsPerRule = new TreeMap<>();
    for (Violation violation : violations) {
      violatedAssetsPerRule.put(violation.getRule().getRuleId(), violation.getAssetId());
    }
//    violatedAssetsPerRule.forEach((ruleId, asset) -> System.out.println(ruleId + " : \"" + asset + "\""));
    return violatedAssetsPerRule;
  }

}
