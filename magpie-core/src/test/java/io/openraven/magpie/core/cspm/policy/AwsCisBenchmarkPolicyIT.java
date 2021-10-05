package io.openraven.magpie.core.cspm.policy;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static io.openraven.magpie.core.cspm.analysis.IgnoredRule.IgnoredReason.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AwsCisBenchmarkPolicyIT extends AbstractPolicyIT {

  private static final String RULES_ASSETS_PATH = "/assets/aws-cis-violated-rule-assets.yaml";
  private static final String POLICY_ID = "opnrvn-p-1";

  @Test
  public void testAwsCisRules() throws Exception {
    // when
    var policyContext = policyMap.get(POLICY_ID);
    var scanResults = analyzePolicies(List.of(policyContext));

    // then
    // Assert ignored rules
    var ignoredRules = scanResults.getIgnoredRules();
    assertEquals(6, getIgnoredRulesByType(ignoredRules, MANUAL_CONTROL).size());
    assertEquals(0, getIgnoredRulesByType(ignoredRules, DISABLED).size());
    assertEquals(17, getIgnoredRulesByType(ignoredRules, MISSING_ASSET).size());

    // Assert violations
    var violations = scanResults.getViolations();
    var violatedAssetsPerRule = getViolatedAssetsByRule(violations);
    var rulesTestAssets = getRulesTestAssets(RULES_ASSETS_PATH);

    var copyMap = new HashMap<>(rulesTestAssets);
    violatedAssetsPerRule.forEach(copyMap::remove);
    assertEquals(Collections.emptyMap(), copyMap);

    assertEquals(violatedAssetsPerRule, rulesTestAssets);

    assertEquals(14, violatedAssetsPerRule.size());
    assertEquals(38, getMissedViolationRules(policyContext, violatedAssetsPerRule).size()); // <-- Not testing
  }
}
