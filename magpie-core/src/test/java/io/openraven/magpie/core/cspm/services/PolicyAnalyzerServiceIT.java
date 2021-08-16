package io.openraven.magpie.core.cspm.services;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.openraven.magpie.api.cspm.*;
import io.openraven.magpie.plugins.persist.FlywayMigrationService;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyAnalyzerServiceIT {

  // Tested class
  private PolicyAnalyzerServiceImpl policyAnalyzerService = new PolicyAnalyzerServiceImpl();

  private static Jdbi jdbi;

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

    jdbiSetuo(jdbcDatabaseContainer);
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
    when(policy.getPolicyName()).thenReturn(policyName);
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
    when(policy.getPolicyName()).thenReturn(policyName);
    when(policy.getCloudProvider()).thenReturn(cloudProvider);
    when(policyContext.getPolicy()).thenReturn(policy);

    // when
    ScanResults analyze = initAndAnalyzePolicies();

    // then
    assertEquals(1, analyze.getPolicies().size());
    assertEquals(0, analyze.getViolations().size());
    assertEquals(0, analyze.getIgnoredRules().size());

    assertTrue( loggerStubAppender.contains(
      String.format("There was no assets detected for cloudProvider: %s, policy: %s", cloudProvider, policyName),
      Level.WARN));
  }

  @Test
  void processingPolicyWithAWSAssetsWithDisabledRules() throws Exception {
    // given
    String policyName = "no-rules-policy";
    String cloudProvider = "AWS";
    String testRuleName = "test-rule-name";

    when(policy.isEnabled()).thenReturn(true);
    when(policy.getPolicyName()).thenReturn(policyName);
    when(policy.getCloudProvider()).thenReturn(cloudProvider);
    when(policy.getRules()).thenReturn(List.of(rule));
    when(rule.isEnabled()).thenReturn(false); // <-- disabled
    when(rule.getRuleName()).thenReturn(testRuleName);
    when(policyContext.getPolicy()).thenReturn(policy);

    // when
    ScanResults analyze = initAndAnalyzePolicies();

    // then
    assertEquals(1, analyze.getPolicies().size());
    assertEquals(0, analyze.getViolations().size());
    assertEquals(1, analyze.getIgnoredRules().size());
    assertEquals(ScanResults.IgnoredReason.DISABLED.getReason(),
      analyze.getIgnoredRules().get(policyContext).get(rule).getReason());

    assertTrue(loggerStubAppender.contains(String.format("Rule '%s' disabled", testRuleName), Level.INFO));
  }

  @Test
  void processingPolicyWithAWSAssetsWithMunuallyControlledRules() throws Exception {
    // given
    String policyName = "no-rules-policy";
    String cloudProvider = "AWS";
    String testRuleName = "test-rule-name";

    when(policy.isEnabled()).thenReturn(true);
    when(policy.getPolicyName()).thenReturn(policyName);
    when(policy.getCloudProvider()).thenReturn(cloudProvider);
    when(policy.getRules()).thenReturn(List.of(rule));
    when(rule.isEnabled()).thenReturn(true); // <-- Enabled now
    when(rule.isManualControl()).thenReturn(true); // <-- Manual
    when(rule.getRuleName()).thenReturn(testRuleName);
    when(policyContext.getPolicy()).thenReturn(policy);

    // when
    ScanResults analyze = initAndAnalyzePolicies();

    // then
    assertEquals(1, analyze.getPolicies().size());
    assertEquals(0, analyze.getViolations().size());
    assertEquals(1, analyze.getIgnoredRules().size());
    assertEquals(ScanResults.IgnoredReason.MANUAL_CONTROL.getReason(),
      analyze.getIgnoredRules().get(policyContext).get(rule).getReason());

    assertTrue(loggerStubAppender.contains(String.format("Rule not analyzed (manually controlled) - %s", testRuleName),
      Level.INFO));
  }

  @Test
  void processingPolicyWithAWSAssetsWithMissedAssetRules() throws Exception {
    // given
    String policyName = "no-rules-policy";
    String cloudProvider = "AWS";
    String testRuleName = "test-rule-name";
    String missedAssets = "AWS::CloudTrail::Trail, AWS::S3::Bucket";

    when(policyContext.getPolicy()).thenReturn(policy);
    when(policy.isEnabled()).thenReturn(true);
    when(policy.getPolicyName()).thenReturn(policyName);
    when(policy.getCloudProvider()).thenReturn(cloudProvider);
    when(policy.getRules()).thenReturn(List.of(rule));
    when(rule.isEnabled()).thenReturn(true); // <-- Enabled now
    when(rule.isManualControl()).thenReturn(false); // <-- NOT Manual
    when(rule.getRuleName()).thenReturn(testRuleName);
    when(rule.getSql()).thenReturn(getResourceAsString("/sql/rule-sql-with-missed-asset.sql"));

    // when
    ScanResults analyze = initAndAnalyzePolicies();

    // then
    assertEquals(1, analyze.getPolicies().size());
    assertEquals(0, analyze.getViolations().size());
    assertEquals(1, analyze.getIgnoredRules().size());
    assertEquals(ScanResults.IgnoredReason.MISSING_ASSET.getReason(),
      analyze.getIgnoredRules().get(policyContext).get(rule).getReason());
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
    String policyId = "somePolicyId";
    String ruleId = "someRuleId";
    String ruleDescription = "some rule description";

    when(policyContext.getPolicy()).thenReturn(policy);
    when(policy.isEnabled()).thenReturn(true);
    when(policy.getPolicyName()).thenReturn(policyName);
    when(policy.getCloudProvider()).thenReturn(cloudProvider);
    when(policy.getRules()).thenReturn(List.of(rule));
    when(policy.getId()).thenReturn(policyId);
    when(policy.getDescription()).thenReturn(ruleDescription);
    when(rule.isEnabled()).thenReturn(true); // <-- Enabled now
    when(rule.getId()).thenReturn(ruleId); // <-- Enabled now
    when(rule.isManualControl()).thenReturn(false); // <-- NOT Manual
    when(rule.getRuleName()).thenReturn(testRuleName);
    when(rule.getSql()).thenReturn(getResourceAsString("/sql/rule-sql-with-available-asset.sql"));

    // when
    ScanResults analyze = initAndAnalyzePolicies();

    // then
    assertEquals(1, analyze.getPolicies().size());
    assertEquals(1, analyze.getViolations().size());
    assertEquals(1, analyze.getNumOfViolations());
    assertEquals(0, analyze.getIgnoredRules().size());

    List<Violation> violations = analyze.getViolations().get(policyContext);
    assertEquals(1, violations.size());
    Violation violation = violations.get(0);
    assertEquals(policyId, violation.getPolicyId());
    assertEquals(violatedAssetId, violation.getAssetId());
    assertEquals(ruleId, violation.getRuleId());
    assertEquals(ruleDescription, violation.getInfo());

    assertTrue(loggerStubAppender.contains(String.format("Analyzing rule - %s", testRuleName), Level.INFO));
  }

  private static void jdbiSetuo(JdbcDatabaseContainer jdbcDatabaseContainer) {
    jdbi = Jdbi.create(jdbcDatabaseContainer.getJdbcUrl(),
      jdbcDatabaseContainer.getUsername(),
      jdbcDatabaseContainer.getPassword())
      .installPlugin(new PostgresPlugin())
      .installPlugin(new SqlObjectPlugin());
  }

  private static void persistanceSetup(JdbcDatabaseContainer jdbcDatabaseContainer) {
    PersistConfig persistConfig = new PersistConfig();
    persistConfig.setHostname("localhost");
    persistConfig.setDatabaseName(jdbcDatabaseContainer.getDatabaseName());
    persistConfig.setPort(String.valueOf(jdbcDatabaseContainer.getFirstMappedPort()));
    persistConfig.setUser(jdbcDatabaseContainer.getUsername());
    persistConfig.setPassword(jdbcDatabaseContainer.getUsername());

    pluginConfig = new PluginConfig<>();
    pluginConfig.setConfig(persistConfig);

    FlywayMigrationService.initiateDBMigration(persistConfig);
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
    jdbi.useHandle(handle -> handle.execute(getResourceAsString("/sql/aws-asset-credential-report.sql")));
  }

  private ScanResults initAndAnalyzePolicies() throws Exception {
    policyAnalyzerService.init(config);
    return policyAnalyzerService.analyze(List.of(policyContext));
  }
}
