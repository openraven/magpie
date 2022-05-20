package io.openraven.magpie.core.cspm.services;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.openraven.magpie.core.cspm.analysis.IgnoredRule;
import io.openraven.magpie.core.cspm.analysis.ScanResults;
import io.openraven.magpie.core.cspm.analysis.Violation;
import io.openraven.magpie.core.cspm.model.Policy;
import io.openraven.magpie.core.cspm.model.PolicyContext;
import io.openraven.magpie.core.cspm.model.Rule;
import io.openraven.magpie.plugins.persist.AssetsRepo;
import io.openraven.magpie.plugins.persist.impl.HibernateAssetsRepoImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;
import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.core.config.PluginConfig;
import io.openraven.magpie.core.util.LoggerStubAppender;
import io.openraven.magpie.plugins.persist.PersistConfig;
import io.openraven.magpie.plugins.persist.PersistPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainerProvider;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;

import static io.openraven.magpie.core.cspm.analysis.IgnoredRule.IgnoredReason.DISABLED;
import static java.util.stream.Collectors.groupingBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyAnalyzerServiceIT {

  // Tested class
  private final PolicyAnalyzerServiceImpl policyAnalyzerService = new PolicyAnalyzerServiceImpl();

  private static final String POLICY_UUID = "567a208e-dc8f-40a0-a7bb-ca91b786bead";
  private static final String RULE_UUID = "fd7f5a7a-53f1-d9a3-9684-fe860d9de223";

  private static AssetsRepo assetsRepo;

  @Mock
  private MagpieConfig config;

  @Mock
  private Policy policy;

  @Mock
  private PolicyContext policyContext;

  @Mock
  private Rule rule;

  private static LoggerStubAppender loggerStubAppender;

  private static PluginConfig<PersistConfig> pluginConfig;

  @BeforeAll
  static void setUp() {
    var postgreSQLContainerProvider = new PostgreSQLContainerProvider();
    var jdbcDatabaseContainer = postgreSQLContainerProvider.newInstance();
    jdbcDatabaseContainer.start();

    persistanceSetup(jdbcDatabaseContainer);
    loggerStubSetup();
    populateAssetData();
  }

  @BeforeEach
  void setupLoggerSpy() {
    loggerStubAppender.reset();
    reset(config, policy, policyContext, rule);
    when(config.getPlugins()).thenReturn(Map.of(PersistPlugin.ID, pluginConfig));
  }

  @Test
  void testSkipDisabledPolicy() throws Exception {
    // given
    String policyName = "disabled-policy";
    when(policy.isEnabled()).thenReturn(false);
    when(policy.getName()).thenReturn(policyName);
    when(policyContext.getPolicy()).thenReturn(policy);

    // when
    ScanResults analyze = initAndAnalyzePolicies();

    // then
    assertEquals(1, analyze.getPolicies().size());
    assertEquals(0, analyze.getViolations().size());
    assertEquals(0, analyze.getIgnoredRules().size());

    assertTrue(loggerStubAppender.contains(String.format("Policy '%s' disabled", policyName), Level.INFO));
  }

  @Test
  void skipPolicyWithoutCloudRelatedAssets() throws Exception {
    // given
    String policyName = "no-asset-policy";
    String cloudProvider = "GCP";
    when(policy.isEnabled()).thenReturn(true);
    when(policy.getName()).thenReturn(policyName);
    when(policy.getCloudProvider()).thenReturn(cloudProvider);
    when(policyContext.getPolicy()).thenReturn(policy);

    // when
    ScanResults analyze = initAndAnalyzePolicies();

    // then
    assertEquals(1, analyze.getPolicies().size());
    assertEquals(0, analyze.getViolations().size());
    assertEquals(0, analyze.getIgnoredRules().size());

    assertTrue( loggerStubAppender.contains(
      String.format("No assets found for cloudProvider: %s, policy: %s", cloudProvider, policyName),
      Level.WARN));
  }

  @Test
  void processingPolicyWithAWSAssetsWithDisabledRules() throws Exception {
    // given
    String policyName = "no-rules-policy";
    String cloudProvider = "AWS";
    String testRuleName = "test-rule-name";

    when(policy.isEnabled()).thenReturn(true);
    when(policy.getName()).thenReturn(policyName);
    when(policy.getCloudProvider()).thenReturn(cloudProvider);
    when(policy.getRules()).thenReturn(List.of(rule));
    when(rule.isEnabled()).thenReturn(false); // <-- disabled
    when(rule.getName()).thenReturn(testRuleName);
    when(policyContext.getPolicy()).thenReturn(policy);

    // when
    ScanResults analyze = initAndAnalyzePolicies();
    Map<Policy, List<IgnoredRule>> ignoredRulesMap = remapIgnoredRules(analyze);

    // then
    assertEquals(1, analyze.getPolicies().size());
    assertEquals(0, analyze.getViolations().size());
    assertEquals(1, analyze.getIgnoredRules().size());

    // TODO fix me
    assertEquals(DISABLED.getReason(),
      ignoredRulesMap.get(policy).get(0).getIgnoredReason().getReason());

    assertTrue(loggerStubAppender.contains(String.format("Rule '%s' disabled", testRuleName), Level.INFO));
  }

  @Test
  void processingPolicyWithAWSAssetsWithMunuallyControlledRules() throws Exception {
    // given
    String policyName = "no-rules-policy";
    String cloudProvider = "AWS";
    String testRuleName = "test-rule-name";

    when(policy.isEnabled()).thenReturn(true);
    when(policy.getName()).thenReturn(policyName);
    when(policy.getCloudProvider()).thenReturn(cloudProvider);
    when(policy.getRules()).thenReturn(List.of(rule));
    when(rule.isEnabled()).thenReturn(true); // <-- Enabled now
    when(rule.isManualControl()).thenReturn(true); // <-- Manual
    when(rule.getName()).thenReturn(testRuleName);
    when(policyContext.getPolicy()).thenReturn(policy);

    // when
    ScanResults analyze = initAndAnalyzePolicies();

    // then
    assertEquals(1, analyze.getPolicies().size());
    assertEquals(0, analyze.getViolations().size());
    assertEquals(1, analyze.getIgnoredRules().size());
    // TODO fix me
//    assertEquals(ScanResults.IgnoredReason.MANUAL_CONTROL.getReason(),
//      analyze.getIgnoredRules().get(policyContext).get(rule).getReason());

    assertTrue(loggerStubAppender.contains(String.format("Rule not analyzed (manually controlled) - %s", testRuleName),
      Level.INFO));
  }

  @Test
  void processingPolicyWithAWSAssetsWithMissedAssetRules() throws Exception {
    // given
    String policyName = "no-rules-policy";
    String cloudProvider = "AWS";
    String testRuleName = "test-rule-name";
    String missedAssets = "AWS::CloudTrail::Trail";

    when(policyContext.getPolicy()).thenReturn(policy);
    when(policy.isEnabled()).thenReturn(true);
    when(policy.getName()).thenReturn(policyName);
    when(policy.getCloudProvider()).thenReturn(cloudProvider);
    when(policy.getRules()).thenReturn(List.of(rule));
    when(rule.isEnabled()).thenReturn(true); // <-- Enabled now
    when(rule.isManualControl()).thenReturn(false); // <-- NOT Manual
    when(rule.getName()).thenReturn(testRuleName);
    when(rule.getSql()).thenReturn(getResourceAsString("/sql/rule-sql-with-missed-asset.sql"));

    // when
    ScanResults analyze = initAndAnalyzePolicies();

    // then
    assertEquals(1, analyze.getPolicies().size());
    assertEquals(0, analyze.getViolations().size());
    assertEquals(1, analyze.getIgnoredRules().size());

    // TODO fix me
//    assertEquals(ScanResults.IgnoredReason.MISSING_ASSET.getReason(),
//      analyze.getIgnoredRules().get(policyContext).get(rule).getReason());
//
//
    assertTrue(loggerStubAppender.contains(
      String.format("Missing assets for analyzing the rule, ignoring. [assets=[%s], rule=%s]", missedAssets, testRuleName),
      Level.INFO));
  }

  @Test
  void processingPolicyWithAWSAssetsWithViolatedRule() throws Exception {
    // given
    String policyName = "no-rules-policy";
    String cloudProvider = "AWS";
    String testRuleName = "test-rule-name";
    String violatedAssetId = "arn:aws:iam::723176279592:root";
    UUID policyId = UUID.fromString(POLICY_UUID);;
    UUID ruleId = UUID.fromString(RULE_UUID);
    String ruleDescription = "some rule description";

    when(policyContext.getPolicy()).thenReturn(policy);
    when(policy.isEnabled()).thenReturn(true);
    when(policy.getName()).thenReturn(policyName);
    when(policy.getCloudProvider()).thenReturn(cloudProvider);
    when(policy.getRules()).thenReturn(List.of(rule));
    when(policy.getId()).thenReturn(policyId);
    when(rule.getDescription()).thenReturn(ruleDescription);
    when(rule.isEnabled()).thenReturn(true); // <-- Enabled now
    when(rule.getId()).thenReturn(ruleId); // <-- Enabled now
    when(rule.isManualControl()).thenReturn(false); // <-- NOT Manual
    when(rule.getName()).thenReturn(testRuleName);
    when(rule.getSql()).thenReturn(getResourceAsString("/sql/rule-sql-with-available-asset.sql"));

    // when
    ScanResults analyze = initAndAnalyzePolicies();

    // then
    assertEquals(1, analyze.getPolicies().size());
    assertEquals(1, analyze.getViolations().size());
    assertEquals(0, analyze.getIgnoredRules().size());

    List<Violation> violations = analyze.getViolations();
    assertEquals(1, violations.size());
    Violation violation = violations.get(0);
    assertEquals(policyId, violation.getPolicy().getId());
    assertEquals(violatedAssetId, violation.getAssetId());
    assertEquals(ruleId, violation.getRule().getId());
    assertEquals(ruleDescription, violation.getInfo());

    assertTrue(loggerStubAppender.contains(String.format("Analyzing rule - %s", testRuleName), Level.INFO));
  }

  private static void persistanceSetup(JdbcDatabaseContainer jdbcDatabaseContainer) {
    PersistConfig persistConfig = new PersistConfig();
    persistConfig.setHostname("localhost");
    persistConfig.setDatabaseName(jdbcDatabaseContainer.getDatabaseName());
    persistConfig.setPort(String.valueOf(jdbcDatabaseContainer.getFirstMappedPort()));
    persistConfig.setUser(jdbcDatabaseContainer.getUsername());
    persistConfig.setPassword(jdbcDatabaseContainer.getUsername());

    assetsRepo = new HibernateAssetsRepoImpl(persistConfig);

    pluginConfig = new PluginConfig<>();
    pluginConfig.setConfig(persistConfig);
  }

  private static void loggerStubSetup() {
    Logger logger = (Logger) LoggerFactory.getLogger("io.openraven.magpie.core.cspm.services.PolicyAnalyzerServiceImpl");
    loggerStubAppender = new LoggerStubAppender();
    loggerStubAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
    logger.setLevel(Level.INFO);
    logger.addAppender(loggerStubAppender);
    loggerStubAppender.start();
  }

  private static String getResourceAsString(String resourcePath) {
    return new Scanner(
      Objects.requireNonNull(PolicyAnalyzerServiceIT.class.getResourceAsStream(resourcePath)),
      StandardCharsets.UTF_8)
      .useDelimiter("\\A").next();
  }

  private static void populateAssetData() {
    assetsRepo.executeNative(getResourceAsString("/sql/aws-asset-credential-report.sql"));
  }

  private ScanResults initAndAnalyzePolicies() throws Exception {
    policyAnalyzerService.init(config);
    return policyAnalyzerService.analyze(List.of(policyContext));
  }

  private Map<Policy, List<IgnoredRule>> remapIgnoredRules(ScanResults analyze) {
    return analyze.getIgnoredRules().stream()
      .collect(groupingBy(IgnoredRule::getPolicy));
  }

}
