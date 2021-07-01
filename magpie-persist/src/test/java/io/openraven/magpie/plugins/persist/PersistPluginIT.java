package io.openraven.magpie.plugins.persist;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openraven.magpie.api.MagpieEnvelope;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainerProvider;

import java.util.List;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.persist.TestUtils.getResourceAsString;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PersistPluginIT {

  private static final String SELECT_TABLES_META =
    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'";
  private static final String SELECT_ASSET_TABLE = "SELECT * FROM assets";

  private static Jdbi jdbi;
  private static PersistConfig persistConfig;
  private final PersistPlugin persistPlugin = new PersistPlugin();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  private MagpieEnvelope magpieEnvelope;

  @BeforeAll
  static void setup() {
    var postgreSQLContainerProvider = new PostgreSQLContainerProvider();
    var jdbcDatabaseContainer = postgreSQLContainerProvider.newInstance();
    jdbcDatabaseContainer.start();

    jdbi = Jdbi.create(jdbcDatabaseContainer.getJdbcUrl(),
      jdbcDatabaseContainer.getUsername(),
      jdbcDatabaseContainer.getPassword())
      .installPlugin(new PostgresPlugin())
      .installPlugin(new SqlObjectPlugin());

    persistConfig = new PersistConfig();
    persistConfig.setHostname("localhost");
    persistConfig.setDatabaseName(jdbcDatabaseContainer.getDatabaseName());
    persistConfig.setPort(String.valueOf(jdbcDatabaseContainer.getFirstMappedPort()));
    persistConfig.setUser(jdbcDatabaseContainer.getUsername());
    persistConfig.setPassword(jdbcDatabaseContainer.getUsername());
  }

  @Test
  void whenPersistPluginIntiFlywaySetupSchemaProperly() {
    // given
    List<String> tablesBefore = jdbi.withHandle(this::selectTables);
    assertEquals(0, tablesBefore.size());

    // when
    persistPlugin.init(persistConfig, LoggerFactory.getLogger(this.getClass()));

    // then
    List<String> tablesAfter = jdbi.withHandle(this::selectTables);
    assertEquals(2, tablesAfter.size());
    assertTrue(tablesAfter.contains("flyway_schema_history"));
    assertTrue(tablesAfter.contains("assets"));
  }

  private List<String> selectTables(Handle handle) {
    return handle
      .createQuery(SELECT_TABLES_META)
      .mapTo(String.class)
      .stream()
      .collect(Collectors.toList());
  }

  @Test
  void whenPersistPluginProcessEnvelopeDataShouldBeSaved() throws Exception {
    // given
    persistPlugin.init(persistConfig, LoggerFactory.getLogger(this.getClass()));
    ObjectNode contents = objectMapper.readValue(
      getResourceAsString("/documents/envelope-content.json"), ObjectNode.class);
    Mockito.when(magpieEnvelope.getContents()).thenReturn(contents);

    // when
    persistPlugin.accept(magpieEnvelope);

    // then
    List<AssetModel> assets = jdbi.withHandle(this::queryAssetTable);
    assertEquals(1, assets.size());
    assertAsset(assets.get(0));
  }

  private void assertAsset(AssetModel assetModel) {
    assertEquals("4jUz_CPXMG-Z7f8oJltkPg", assetModel.getDocumentId());
    assertEquals("arn:aws:iam::000000000000:group/Accountants", assetModel.getAssetId());
    assertEquals("Accountants", assetModel.getResourceName());
    assertEquals("y9xomssf3o582439fxep", assetModel.getResourceId());
    assertEquals("AWS::IAM::Group", assetModel.getResourceType());
    assertEquals("us-west-1", assetModel.region);
    assertNull(assetModel.getProjectId());
    assertEquals("account", assetModel.getAccountId());
    assertNull(assetModel.getCreatedIso());
    assertEquals("2021-06-23T09:44:50.397706Z", assetModel.getUpdatedIso().toString());
    assertNull(assetModel.getDiscoverySessionId());
    assertEquals(0, assetModel.getMaxSizeInBytes());
    assertEquals(0, assetModel.getSizeInBytes());
    assertEquals("{\"arn\": \"arn:aws:iam::000000000000:group/Accountants\", \"path\": \"/\", \"groupId\": \"y9xomssf3o582439fxep\", \"groupName\": \"Accountants\", \"createDate\": null}",
      assetModel.getConfiguration());
    assertEquals("{\"inlinePolicies\": [{\"name\": \"inlineDataAccess\", \"policyDocument\": \"{\\\"Version\\\":\\\"2012-10-17\\\",\\\"Statement\\\":[{\\\"Effect\\\":\\\"Deny\\\",\\\"Action\\\":[\\\"dynamodb:DeleteItem\\\",\\\"dynamodb:GetItem\\\"],\\\"Resource\\\":\\\"*\\\"}]}\"}], \"attachedPolicies\": [{\"arn\": \"arn:aws:iam::000000000000:policy/managedDataAccess\", \"name\": \"managedDataAccess\"}]}",
      assetModel.getSupplementaryConfiguration());
    assertEquals("{}", assetModel.getTags());
    assertEquals("{}", assetModel.getDiscoveryMeta());
  }

  private List<AssetModel> queryAssetTable(Handle handle) {
    return handle
      .createQuery(SELECT_ASSET_TABLE)
      .mapToBean(AssetModel.class)
      .stream()
      .collect(Collectors.toList());
  }
}
