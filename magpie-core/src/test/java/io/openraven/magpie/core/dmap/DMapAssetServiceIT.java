package io.openraven.magpie.core.dmap;

import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.core.config.PluginConfig;
import io.openraven.magpie.core.dmap.model.EC2Target;
import io.openraven.magpie.core.dmap.model.VpcConfig;
import io.openraven.magpie.core.dmap.service.DMapAssetService;
import io.openraven.magpie.core.dmap.service.DMapAssetServiceImpl;
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

import static io.openraven.magpie.core.dmap.Util.getResourceAsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DMapAssetServiceIT {

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
    DMapAssetService dMapLambdaService = new DMapAssetServiceImpl(config);
    Map<VpcConfig, List<EC2Target>> vpcConfigListMap = dMapLambdaService.groupScanTargets();

    assertEquals(4, vpcConfigListMap.size());

    // Group 1
    List<EC2Target> ec2BatchOneTarget = vpcConfigListMap.get(
      getVpcConfg("subnet-0e0c65d2da128849f", List.of("sg-00a0b7c747d5bc8af")));
    assertEquals(1, ec2BatchOneTarget.size());
    assertTrue(ec2BatchOneTarget.contains(new EC2Target("i-0b2bff5afdc58ef7a", "10.128.0.253")));

    // Group 2
    List<EC2Target> ec2BatchThreeTargets = vpcConfigListMap.get(
      getVpcConfg("subnet-022a263e58d1ada34", List.of("sg-07a6077c9af3c6801")));
    assertEquals(3, ec2BatchThreeTargets.size());
    assertTrue(ec2BatchThreeTargets.contains(new EC2Target("i-02d707a66b2c4d632", "10.128.111.91")));
    assertTrue(ec2BatchThreeTargets.contains(new EC2Target("i-0bcfc92093c876165", "10.128.111.142")));
    assertTrue(ec2BatchThreeTargets.contains(new EC2Target("i-0c4779103ad377ecb", "10.128.104.217")));

    // Group 3
    List<EC2Target> ec2BatchAnotherSG = vpcConfigListMap.get(
      getVpcConfg("subnet-022a263e58d1ada34", List.of("sg-00a0b7c747d5bc8af")));
    assertEquals(3, ec2BatchAnotherSG.size());
    assertTrue(ec2BatchAnotherSG.contains(new EC2Target("i-0a85540439e24fa0c", "10.128.101.122")));
    assertTrue(ec2BatchAnotherSG.contains(new EC2Target("i-0b08e2ff264f600e2", "10.128.104.104")));
    assertTrue(ec2BatchAnotherSG.contains(new EC2Target("i-0a4314cb5c5f3f65b", "10.128.99.21")));

    // Group 4
    List<EC2Target> ec2BatchTwoSG = vpcConfigListMap.get(
      getVpcConfg("subnet-022a263e58d1ada34", List.of("sg-07a6077c9af3c6801", "sg-00a0b7c747d5bc8af")));
    assertEquals(1, ec2BatchTwoSG.size());
    assertTrue(ec2BatchTwoSG.contains(new EC2Target("i-072c5dcd933c2218a", "10.128.110.70")));
  }

  private VpcConfig getVpcConfg(String subnetId, List<String> securityGroups) {
    return new VpcConfig("us-west-1", subnetId, securityGroups);
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

  private static void populateAssetData() {
    jdbi.useHandle(handle -> handle.execute(getResourceAsString("/sql/aws-ec2-dmap-scan-targets.sql")));
  }
}
