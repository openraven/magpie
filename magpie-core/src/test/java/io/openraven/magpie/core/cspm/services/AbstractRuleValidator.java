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
import io.openraven.magpie.data.Resource;
import io.openraven.magpie.plugins.persist.AssetsRepo;
import io.openraven.magpie.plugins.persist.PersistConfig;
import io.openraven.magpie.plugins.persist.PersistPlugin;
import io.openraven.magpie.plugins.persist.impl.HibernateAssetsRepoImpl;
import org.junit.jupiter.api.AfterEach;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainerProvider;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class AbstractRuleValidator {

  private static final String DEFAULT_SCHEMA = "magpie_it";
  private static final String DEFAULT_SECURITY_RULES_REPO = "https://github.com/openraven/security-rules.git";
  private static final String REPOSITORY_PROPERTY = "repository";
  private static final PolicyAcquisitionServiceImpl policyAcquisitionService = new PolicyAcquisitionServiceImpl();

  private static PluginConfig<PersistConfig> pluginConfig;
  private static AssetsRepo assetsRepo;

  protected static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory()).registerModule(new JavaTimeModule());
  protected static Map<String, Rule> ruleMap;
  protected static PolicyAnalyzerServiceImpl analyzerService = new PolicyAnalyzerServiceImpl();

  // Before all extended
  static {
    var postgreSQLContainerProvider = new PostgreSQLContainerProvider();
    var jdbcDatabaseContainer = postgreSQLContainerProvider.newInstance();
    jdbcDatabaseContainer.start();

    persistenceSetup(jdbcDatabaseContainer);
    ruleMap = loadRules();

    MagpieConfig magpieConfig = new MagpieConfig();
    magpieConfig.setPlugins(Map.of(PersistPlugin.ID, pluginConfig));

    analyzerService.init(magpieConfig);
  }

  private static void persistenceSetup(JdbcDatabaseContainer jdbcDatabaseContainer) {
    PersistConfig persistConfig = new PersistConfig();
    persistConfig.setHostname("localhost");
    persistConfig.setSchema(DEFAULT_SCHEMA);
    persistConfig.setDatabaseName(jdbcDatabaseContainer.getDatabaseName());
    persistConfig.setPort(String.valueOf(jdbcDatabaseContainer.getFirstMappedPort()));
    persistConfig.setUser(jdbcDatabaseContainer.getUsername());
    persistConfig.setPassword(jdbcDatabaseContainer.getUsername());
    assetsRepo = new HibernateAssetsRepoImpl(persistConfig);

    pluginConfig = new PluginConfig<>();
    pluginConfig.setConfig(persistConfig);
  }

  protected static Map<String, Rule> loadRules() {
    PolicyConfig policyConfig = new PolicyConfig();
    policyConfig.setRepositories(List.of(getSecurityRulesRepository()));

    var persistConfig = new PluginConfig();
    var cfg = new PersistConfig();
    cfg.setSchema(DEFAULT_SCHEMA);
    persistConfig.setConfig(cfg);
    MagpieConfig magpieConfig = new MagpieConfig();
    magpieConfig.getPlugins().put("magpie.persist", persistConfig);
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

      try {
        var assetModel = MAPPER.treeToValue(envelope.getContents(), Resource.class);
        assetsRepo.upsert(assetModel);
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException(e);
      }
    });
  }

  protected void analyzeRule(List<Violation> violations,
                             List<IgnoredRule> ignoredRules,
                             Policy policy,
                             Rule rule) {
    analyzerService.executeRule(violations, ignoredRules, policy, rule);
  }

  @AfterEach
  protected void cleanupAssets() {
    assetsRepo.executeNative("DELETE FROM "  + DEFAULT_SCHEMA + ".aws; DELETE FROM " + DEFAULT_SCHEMA +  ".gcp");
  }

  protected static String getTargetProjectDirectoryPath() {
    return policyAcquisitionService.getTargetProjectDirectoryPath(getSecurityRulesRepository()).toString();
  }

  /**
   * If VM option 'repository' is set, this will be used as the
   * security rules location, helpful for overriding the clone from Github.
   * E.g using a local repo. e.g -Drepository="~/projects/security-rules"
   *
   * @return URL to the security rules repo or specified repo path
   */
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
    private String cloudProvider;

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

    public String getCloudProvider() {
      return cloudProvider;
    }

    public void setCloudProvider(String cloudProvider) {
      this.cloudProvider = cloudProvider;
    }
  }

}
