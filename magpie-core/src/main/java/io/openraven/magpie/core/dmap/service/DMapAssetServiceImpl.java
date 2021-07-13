package io.openraven.magpie.core.dmap.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.core.config.ConfigException;
import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.core.dmap.Util;
import io.openraven.magpie.core.dmap.model.DMapTarget;
import io.openraven.magpie.core.dmap.model.EC2Target;
import io.openraven.magpie.core.dmap.model.VpcConfig;
import io.openraven.magpie.plugins.persist.PersistConfig;
import io.openraven.magpie.plugins.persist.PersistPlugin;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.*;

public class DMapAssetServiceImpl implements DMapAssetService {

  private final ObjectMapper mapper = new ObjectMapper();
  private final Jdbi jdbi;

  public DMapAssetServiceImpl(MagpieConfig config) {
    this.jdbi = initJdbiClient(config);
  }

  @Override
  public Map<VpcConfig, List<EC2Target>> groupScanTargets() {
    String query = Util.getResourceAsString("/sql/dmap-asset-grouping.sql");
    List<DMapTarget> scanTargets = jdbi.withHandle(handle -> handle.createQuery(query)
      .map((rs, ctx) ->
        new DMapTarget(
          rs.getString("resource_id"),
          rs.getString("region"),
          rs.getString("subnet_id"),
          rs.getString("private_ip_address"),
          List.of(rs.getString("security_group").split(","))))
      .list());

    return scanTargets
      .stream()
      .collect(groupingBy(
        dmapTarget -> new VpcConfig(dmapTarget.getRegion(), dmapTarget.getSubnetId(), dmapTarget.getSecurityGroups()),
        mapping(dmapTarget -> new EC2Target(dmapTarget.getResourceId(), dmapTarget.getPrivateIpAddress()), toList())
      ));
  }

  private Jdbi initJdbiClient(MagpieConfig config) {
    final Jdbi jdbi;
    final var rawPersistConfig = config.getPlugins().get(PersistPlugin.ID);
    if (rawPersistConfig == null) {
      throw new ConfigException(String.format("Config file does not contain %s configuration", PersistPlugin.ID));
    }

    try {
      final PersistConfig persistConfig = mapper.treeToValue(mapper.valueToTree(rawPersistConfig.getConfig()), PersistConfig.class);

      String url = String.format("jdbc:postgresql://%s:%s/%s", persistConfig.getHostname(), persistConfig.getPort(), persistConfig.getDatabaseName());
      jdbi = Jdbi.create(url, persistConfig.getUser(), persistConfig.getPassword())
        .installPlugin(new PostgresPlugin());
    } catch (JsonProcessingException e) {
      throw new ConfigException("Cannot instantiate PersistConfig while initializing PolicyAnalyzerService", e);
    }
    return jdbi;
  }
}
