package io.openraven.magpie.core.cspm.services;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.core.config.PluginConfig;
import io.openraven.magpie.core.config.PolicyConfig;
import io.openraven.magpie.core.cspm.analysis.IgnoredRule;
import io.openraven.magpie.core.cspm.analysis.ScanResults;
import io.openraven.magpie.core.util.LoggerStubAppender;
import io.openraven.magpie.plugins.persist.*;
import io.openraven.magpie.plugins.persist.mapper.AssetMapper;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainerProvider;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

import static io.openraven.magpie.core.cspm.analysis.IgnoredRule.IgnoredReason.MISSING_ASSET;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyAnalyzerServiceImplTest {

  private static ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  private static Jdbi jdbi;

  private static LoggerStubAppender loggerStubAppender;

  private static PluginConfig<PersistConfig> pluginConfig;

  private static AssetsRepo assetsRepo;

  @Mock
  private MagpieConfig magpieConfig;

  @BeforeAll
  static void setUp() throws JsonProcessingException {
    var postgreSQLContainerProvider = new PostgreSQLContainerProvider();
    var jdbcDatabaseContainer = postgreSQLContainerProvider.newInstance();
    jdbcDatabaseContainer.start();

    jdbiSetuo(jdbcDatabaseContainer);
    persistanceSetup(jdbcDatabaseContainer);
    loggerStubSetup();
    populateAssetData();
  }

  @Test
  void testSecurityRules() throws Exception {

    PolicyConfig policyConfig = new PolicyConfig();
    policyConfig.setRepositories(List.of("https://github.com/openraven/security-rules.git"));

    when(magpieConfig.getPolicies()).thenReturn(policyConfig);

    var policyAcquisitionService = new PolicyAcquisitionServiceImpl();
    policyAcquisitionService.init(magpieConfig);
    var policies = policyAcquisitionService.loadPolicies();

    assertNotNull(policies);

    when(magpieConfig.getPlugins()).thenReturn(Map.of(PersistPlugin.ID, pluginConfig));

    var analyzerService = new PolicyAnalyzerServiceImpl();
    analyzerService.init(magpieConfig);
    ScanResults scanResults = analyzerService.analyze(policies);

    assertNotNull(scanResults);

    assertEquals(41, scanResults.getViolations().size());

    var missingAssetCounter = 0;
    for (IgnoredRule ignoredRule : scanResults.getIgnoredRules()) {
      if (MISSING_ASSET.equals(ignoredRule.getIgnoredReason())) {
        System.out.println("Rule: " + ignoredRule.getRule().getFileName() + " rulename: " + ignoredRule.getRule().getRuleName());
        missingAssetCounter++;
      }
    }
    assertEquals(0, missingAssetCounter);
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
    assetsRepo = new AssetsRepo(persistConfig);

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

  private static void populateAssetData() throws JsonProcessingException {
    AssetMapper assetMapper = new AssetMapper();
    String assetsJson = getResourceAsString("/json/test-assets.json");

    List<ObjectNode> resources = objectMapper.readValue(assetsJson, new TypeReference<List<ObjectNode>>(){});

    resources.forEach(asset -> {
      MagpieEnvelope envelope = new MagpieEnvelope();
      envelope.setContents(asset);

      AssetModel assetModel = assetMapper.map(envelope);
      assetsRepo.upsert(assetModel);
    });
  }
}
