package io.openraven.magpie.core.cspm.policy;

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
import io.openraven.magpie.core.cspm.analysis.ScanResults;
import io.openraven.magpie.core.cspm.model.PolicyContext;
import io.openraven.magpie.core.cspm.services.PolicyAcquisitionServiceImpl;
import io.openraven.magpie.core.cspm.services.PolicyAnalyzerServiceImpl;
import io.openraven.magpie.plugins.persist.*;
import io.openraven.magpie.plugins.persist.mapper.AssetMapper;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainerProvider;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class AbstractPolicyIT {

  private static final List<String> REPOSITORIES = List.of("https://github.com/openraven/security-rules.git");
  protected static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory()).registerModule(new JavaTimeModule());
  private static PluginConfig<PersistConfig> pluginConfig;
  private static AssetsRepo assetsRepo;

  protected static Map<String, PolicyContext> policyMap;

  @BeforeAll
  static void setUp() {
    var postgreSQLContainerProvider = new PostgreSQLContainerProvider();
    var jdbcDatabaseContainer = postgreSQLContainerProvider.newInstance();
    jdbcDatabaseContainer.start();

    persistenceSetup(jdbcDatabaseContainer);
    policyMap = loadPolicies();
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

  protected static Map<String, PolicyContext> loadPolicies() {
    PolicyConfig policyConfig = new PolicyConfig();
    policyConfig.setRepositories(REPOSITORIES);

    MagpieConfig magpieConfig = new MagpieConfig();
    magpieConfig.setPolicies(policyConfig);

    var policyAcquisitionService = new PolicyAcquisitionServiceImpl();
    policyAcquisitionService.init(magpieConfig);
    var policies = policyAcquisitionService.loadPolicies();

    assertNotNull(policies);

    return policies
      .stream()
      .collect(Collectors.toMap(
        policyContext -> policyContext.getPolicy().getPolicyId(),
        Function.identity()));
  }

  protected ScanResults analyzePolicies(List<PolicyContext> policies) throws Exception {
    MagpieConfig magpieConfig = new MagpieConfig();
    magpieConfig.setPlugins(Map.of(PersistPlugin.ID, pluginConfig));

    var analyzerService = new PolicyAnalyzerServiceImpl();
    analyzerService.init(magpieConfig);
    ScanResults scanResults = analyzerService.analyze(policies);
    assertNotNull(scanResults);
    return scanResults;
  }

  protected void populateAssetData(String assetsResourcePath) throws JsonProcessingException {
    AssetMapper assetMapper = new AssetMapper();
    String assetsJson = getResourceAsString(assetsResourcePath);

    List<ObjectNode> resources = MAPPER.readValue(assetsJson, new TypeReference<List<ObjectNode>>(){});

    resources.forEach(asset -> {
      MagpieEnvelope envelope = new MagpieEnvelope();
      envelope.setContents(asset);

      AssetModel assetModel = assetMapper.map(envelope);
      assetsRepo.upsert(assetModel);
    });
  }

  protected Map<String, String> getRulesTestAssets(String rulesAssetsYamlPath) throws JsonProcessingException {
    return MAPPER.readValue(getResourceAsString(rulesAssetsYamlPath), new TypeReference<Map<String, String>>(){});
  }

  private static String getResourceAsString(String resourcePath) {
    return new Scanner(
      Objects.requireNonNull(AbstractPolicyIT.class.getResourceAsStream(resourcePath)),
      StandardCharsets.UTF_8)
      .useDelimiter("\\A").next();
  }

}
