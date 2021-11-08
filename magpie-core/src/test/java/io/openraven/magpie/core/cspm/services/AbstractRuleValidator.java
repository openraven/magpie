package io.openraven.magpie.core.cspm.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.core.config.PluginConfig;
import io.openraven.magpie.core.config.PolicyConfig;
import io.openraven.magpie.core.cspm.analysis.IgnoredRule;
import io.openraven.magpie.core.cspm.analysis.Violation;
import io.openraven.magpie.core.cspm.model.Policy;
import io.openraven.magpie.core.cspm.model.PolicyContext;
import io.openraven.magpie.core.cspm.model.Rule;
import io.openraven.magpie.plugins.persist.AssetModel;
import io.openraven.magpie.plugins.persist.AssetsRepo;
import io.openraven.magpie.plugins.persist.FlywayMigrationService;
import io.openraven.magpie.plugins.persist.PersistConfig;
import io.openraven.magpie.plugins.persist.PersistPlugin;
import io.openraven.magpie.plugins.persist.mapper.AssetMapper;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainerProvider;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled
public abstract class AbstractRuleValidator {

  private static final String DEFAULT_SECURITY_RULES_REPO = "https://github.com/openraven/security-rules.git";
  private static final String REPOSITORY_PROPERTY = "repository";
  private static final AssetMapper ASSET_MAPPER = new AssetMapper();
  private static final PolicyAcquisitionServiceImpl policyAcquisitionService = new PolicyAcquisitionServiceImpl();

  private static PluginConfig<PersistConfig> pluginConfig;
  private static AssetsRepo assetsRepo;
  private static Jdbi jdbi;

  protected static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory()).registerModule(new JavaTimeModule());
  protected static Map<String, Rule> ruleMap;

  // Before all extended
  static {
    var postgreSQLContainerProvider = new PostgreSQLContainerProvider();
    var jdbcDatabaseContainer = postgreSQLContainerProvider.newInstance();
    jdbcDatabaseContainer.start();

    jdbiSetuo(jdbcDatabaseContainer);
    persistenceSetup(jdbcDatabaseContainer);
    ruleMap = loadRules();
  }

  private static void persistenceSetup(JdbcDatabaseContainer jdbcDatabaseContainer) {
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

  private static void jdbiSetuo(JdbcDatabaseContainer jdbcDatabaseContainer) {
    jdbi = Jdbi.create(jdbcDatabaseContainer.getJdbcUrl(),
      jdbcDatabaseContainer.getUsername(),
      jdbcDatabaseContainer.getPassword())
      .installPlugin(new PostgresPlugin())
      .installPlugin(new SqlObjectPlugin());
  }

  protected static Map<String, Rule> loadRules() {
    PolicyConfig policyConfig = new PolicyConfig();
    policyConfig.setRepositories(List.of(getSecurityRulesRepository()));

    MagpieConfig magpieConfig = new MagpieConfig();
    magpieConfig.setPolicies(policyConfig);

    policyAcquisitionService.init(magpieConfig);
    var policies = policyAcquisitionService.loadPolicies();

    assertNotNull(policies);

    return policies
      .stream()
      .map(PolicyContext::getPolicy)
      .map(Policy::getRules)
      .flatMap(Collection::stream)
      .collect(Collectors.toMap(Rule::getRuleId, Function.identity(), (ruleId1, ruleId2) -> ruleId1));
  }

  protected static void populateAssetData(String assetsJson)  {
    List<ObjectNode> resources = null;
    try {
      resources = MAPPER.readValue(assetsJson, new TypeReference<List<ObjectNode>>(){});
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Unable to deserialize assets: " + assetsJson);
    }

    resources.forEach(asset -> {
      MagpieEnvelope envelope = new MagpieEnvelope();
      envelope.setContents(asset);

      AssetModel assetModel = ASSET_MAPPER.map(envelope);
//      assetsRepo.upsert(assetModel); TODO: fix to use Hibernate only
    });
  }

  protected void analyzeRule(List<Violation> violations,
                             List<IgnoredRule> ignoredRules,
                             Rule rule) {
    MagpieConfig magpieConfig = new MagpieConfig();
    magpieConfig.setPlugins(Map.of(PersistPlugin.ID, pluginConfig));

    var analyzerService = new PolicyAnalyzerServiceImpl();
    analyzerService.init(magpieConfig);
    analyzerService.executeRule(violations, ignoredRules, null, rule);
  }

  @AfterEach
  protected void cleanupAssets() {
    jdbi.withHandle(handle -> handle.execute("DELETE FROM assets"));
    assertEquals(0,
      jdbi.withHandle(handle -> handle.createQuery("SELECT count(*) FROM assets")
        .mapTo(Integer.class).one()).intValue());
  }

  protected static String getTargetProjectDirectoryPath() {
    return policyAcquisitionService.getTargetProjectDirectoryPath(getSecurityRulesRepository()).toString();
  }

  private static String getSecurityRulesRepository() {
    String repository = System.getProperty(REPOSITORY_PROPERTY);
    if (repository == null) {
      return DEFAULT_SECURITY_RULES_REPO;
    }
    return repository;
  }

  static class RuleTestResource {
    private String ruleId;
    private String description;
    private Map<String, String> insecureAssets;
    private Map<String, String> secureAssets;

    public String getRuleId() {
      return ruleId;
    }

    public void setRuleId(String ruleId) {
      this.ruleId = ruleId;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public Map<String, String> getInsecureAssets() {
      return insecureAssets;
    }

    public void setInsecureAssets(Map<String, String> insecureAssets) {
      this.insecureAssets = insecureAssets;
    }

    public Map<String, String> getSecureAssets() {
      return secureAssets;
    }

    public void setSecureAssets(Map<String, String> secureAssets) {
      this.secureAssets = secureAssets;
    }
  }

}
