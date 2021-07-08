package io.openraven.magpie.core.cspm.services;

import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.core.config.PluginConfig;
import io.openraven.magpie.core.cspm.VpcConfig;
import io.openraven.magpie.plugins.persist.FlywayMigrationService;
import io.openraven.magpie.plugins.persist.PersistConfig;
import io.openraven.magpie.plugins.persist.PersistPlugin;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DMapServiceIT {

  private static PluginConfig<PersistConfig> pluginConfig;
  private static Jdbi jdbi;

  @Mock
  private MagpieConfig config;

  @BeforeAll
  static void setUp() {
    var postgreSQLContainerProvider = new PostgreSQLContainerProvider();
    var jdbcDatabaseContainer = postgreSQLContainerProvider.newInstance();
    jdbcDatabaseContainer.start();

    jdbiSetuo(jdbcDatabaseContainer);
    persistenceSetup(jdbcDatabaseContainer);
    populateAssetData();

  }

  @BeforeEach
  void setupLoggerSpy() {
    reset(config);
    when(config.getPlugins()).thenReturn(Map.of(PersistPlugin.ID, pluginConfig));
  }

  @Test
  public void testGroupingTargetsReturnProperMap() {
    DMapService dMapService = new DMapServiceImpl(config);
    Map<VpcConfig, List<String>> vpcConfigListMap = dMapService.groupScanTargets();

    assertEquals(4, vpcConfigListMap.size());

    // Group 1
    List<String> ec2BatchOneTarget = vpcConfigListMap.get(
      getVpcConfg("subnet-0e0c65d2da128849f", List.of("sg-00a0b7c747d5bc8af")));
    assertEquals(1, ec2BatchOneTarget.size());
    assertTrue(ec2BatchOneTarget.contains("i-0b2bff5afdc58ef7a"));
    // Group 2
    List<String> ec2BatchThreeTargets = vpcConfigListMap.get(
      getVpcConfg("subnet-022a263e58d1ada34", List.of("sg-07a6077c9af3c6801")));
    assertEquals(3, ec2BatchThreeTargets.size());
    assertTrue(ec2BatchThreeTargets.contains("i-02d707a66b2c4d632"));
    assertTrue(ec2BatchThreeTargets.contains("i-0bcfc92093c876165"));
    assertTrue(ec2BatchThreeTargets.contains("i-0c4779103ad377ecb"));
    // Group 3
    List<String> ec2BatchAnotherSG = vpcConfigListMap.get(
      getVpcConfg("subnet-022a263e58d1ada34", List.of("sg-00a0b7c747d5bc8af")));
    assertEquals(3, ec2BatchAnotherSG.size());
    assertTrue(ec2BatchAnotherSG.contains("i-0a85540439e24fa0c"));
    assertTrue(ec2BatchAnotherSG.contains("i-0b08e2ff264f600e2"));
    assertTrue(ec2BatchAnotherSG.contains("i-0a4314cb5c5f3f65b"));
    // Group 4
    List<String> ec2BatchTwoSG = vpcConfigListMap.get(
      getVpcConfg("subnet-022a263e58d1ada34", List.of("sg-07a6077c9af3c6801", "sg-00a0b7c747d5bc8af")));
    assertEquals(1, ec2BatchTwoSG.size());
    assertTrue(ec2BatchTwoSG.contains("i-072c5dcd933c2218a"));
  }

  private VpcConfig getVpcConfg(String subnetId, List<String> securityGroups) {
    return new VpcConfig(subnetId, securityGroups);
  }


  private static void jdbiSetuo(JdbcDatabaseContainer jdbcDatabaseContainer) {
    jdbi = Jdbi.create(jdbcDatabaseContainer.getJdbcUrl(),
      jdbcDatabaseContainer.getUsername(),
      jdbcDatabaseContainer.getPassword())
      .installPlugin(new PostgresPlugin())
      .installPlugin(new SqlObjectPlugin());
  }

  private static void persistenceSetup(JdbcDatabaseContainer jdbcDatabaseContainer) {
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

  private static String getResourceAsString(String resourcePath) {
    return new Scanner(
      Objects.requireNonNull(PolicyAnalyzerServiceIT.class.getResourceAsStream(resourcePath)),
      StandardCharsets.UTF_8)
      .useDelimiter("\\A").next();
  }

  private static void populateAssetData() {
    jdbi.useHandle(handle -> handle.execute(getResourceAsString("/sql/aws-ec2-dmap-scan-targets.sql")));
  }
}
