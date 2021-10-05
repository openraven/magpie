package io.openraven.magpie.core.cspm.policy;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static io.openraven.magpie.core.cspm.analysis.IgnoredRule.IgnoredReason.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GcpCisBenchmarkPolicyIT extends AbstractPolicyIT {

  private static final String RULES_ASSETS_PATH = "/assets/gcp-cis-violated-rule-assets.yaml";
  private static final String POLICY_ID = "opnrvn-p-2";

  @Test
  public void testGcpRules() throws Exception {
    // when
    var policyContext = policyMap.get(POLICY_ID);
    var scanResults = analyzePolicies(List.of(policyContext));

    // then
    // Assert ignored rules
    var ignoredRules = scanResults.getIgnoredRules();
    assertEquals(15, getIgnoredRulesByType(ignoredRules, MANUAL_CONTROL).size());
    assertEquals(0, getIgnoredRulesByType(ignoredRules, DISABLED).size());
    assertEquals(0, getIgnoredRulesByType(ignoredRules, MISSING_ASSET).size());

    // Assert violations
    var violations = scanResults.getViolations();
    var violatedAssetsPerRule = getViolatedAssetsByRule(violations);
    var rulesTestAssets = getRulesTestAssets(RULES_ASSETS_PATH);

    var copyMap = new HashMap<>(rulesTestAssets);
    violatedAssetsPerRule.forEach(copyMap::remove);
    assertEquals(Collections.emptyMap(), copyMap);
    assertEquals(violatedAssetsPerRule, rulesTestAssets);

    assertEquals(52, violatedAssetsPerRule.size());
    assertEquals(15, getMissedViolationRules(policyContext, violatedAssetsPerRule).size()); // <-- Not testing

  }



}
