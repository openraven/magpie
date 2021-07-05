package io.openraven.magpie.core.cspm.services;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.openraven.magpie.plugins.persist.FlywayMigrationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;
import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.core.config.PluginConfig;
import io.openraven.magpie.core.cspm.Policy;
import io.openraven.magpie.core.cspm.ScanResults;
import io.openraven.magpie.core.util.LoggerStubAppender;
import io.openraven.magpie.plugins.persist.PersistConfig;
import io.openraven.magpie.plugins.persist.PersistPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.PostgreSQLContainerProvider;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyAnalyzerServiceIT {

  // Tested class
  private PolicyAnalyzerServiceImpl policyAnalyzerService = new PolicyAnalyzerServiceImpl();

  @Mock
  private MagpieConfig config;

  @Mock
  private Policy policy;

  @Mock
  private PolicyContext policyContext;

  private static LoggerStubAppender loggerStubAppender;

  private static PluginConfig<PersistConfig> pluginConfig;

  @BeforeAll
  static void setUp() {
    var postgreSQLContainerProvider = new PostgreSQLContainerProvider();
    var jdbcDatabaseContainer = postgreSQLContainerProvider.newInstance();
    jdbcDatabaseContainer.start();

    PersistConfig persistConfig = new PersistConfig();
    persistConfig.setHostname("localhost");
    persistConfig.setDatabaseName(jdbcDatabaseContainer.getDatabaseName());
    persistConfig.setPort(String.valueOf(jdbcDatabaseContainer.getFirstMappedPort()));
    persistConfig.setUser(jdbcDatabaseContainer.getUsername());
    persistConfig.setPassword(jdbcDatabaseContainer.getUsername());

    pluginConfig = new PluginConfig<>();
    pluginConfig.setConfig(persistConfig);

    FlywayMigrationService.initiateDBMigration(persistConfig);

    Logger logger = (Logger) LoggerFactory.getLogger("io.openraven.magpie.core.cspm.services.PolicyAnalyzerServiceImpl");
    loggerStubAppender = new LoggerStubAppender();
    loggerStubAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
    logger.setLevel(Level.INFO);
    logger.addAppender(loggerStubAppender);
    loggerStubAppender.start();
  }

  @BeforeEach
  void setupLoggerSpy() {
    loggerStubAppender.reset();
    reset(config, policy, policyContext);
  }

  @Test
  void testSkipDisabledPolicy() throws Exception {
    // given
    String policyName = "disabled-policy";
    when(policy.isEnabled()).thenReturn(false);
    when(policy.getPolicyName()).thenReturn(policyName);
    when(policyContext.getPolicy()).thenReturn(policy);

    // when
    ScanResults analyze = policyAnalyzerService.analyze(List.of(policyContext));

    // then
    assertEquals(1, analyze.getPolicies().size());
    assertEquals(0, analyze.getViolations().size());
    assertEquals(0, analyze.getIgnoredRules().size());

    assertTrue(loggerStubAppender.contains(
      String.format("Policy '%s' disabled", policyName),
      Level.INFO));
  }

  @Test
  void skipPolicyWithoutCloudRelatedAssets() throws Exception {
    // given
    when(config.getPlugins()).thenReturn(Map.of(PersistPlugin.ID, pluginConfig));

    String policyName = "no-asset-policy";
    String cloudProvider = "GCP";
    when(policy.isEnabled()).thenReturn(true);
    when(policy.getPolicyName()).thenReturn(policyName);
    when(policy.getCloudProvider()).thenReturn(cloudProvider);
    when(policyContext.getPolicy()).thenReturn(policy);

    // when
    policyAnalyzerService.init(config);
    ScanResults analyze = policyAnalyzerService.analyze(List.of(policyContext));

    // then
    assertEquals(1, analyze.getPolicies().size());
    assertEquals(0, analyze.getViolations().size());
    assertEquals(0, analyze.getIgnoredRules().size());

    assertTrue( loggerStubAppender.contains(
      String.format("There was no assets detected for cloudProvider: %s, policy: %s", cloudProvider, policyName),
      Level.WARN));
  }


}
