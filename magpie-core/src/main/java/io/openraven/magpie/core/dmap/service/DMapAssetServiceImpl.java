/*
 * Copyright 2021 Open Raven Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.*;

public class DMapAssetServiceImpl implements DMapAssetService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DMapAssetServiceImpl.class);
  private static final String GROUP_ASSET_SQL_PATH = "/sql/dmap-asset-grouping.sql";
  private static final String QUERY = Util.getResourceAsString(GROUP_ASSET_SQL_PATH);

  private final ObjectMapper mapper = new ObjectMapper();
  private final Jdbi jdbi;

  public DMapAssetServiceImpl(MagpieConfig config) {
    this.jdbi = initJdbiClient(config);
  }

  @Override
  public Map<VpcConfig, List<EC2Target>> groupScanTargets() {
    List<DMapTarget> scanTargets = jdbi.withHandle(handle -> handle.createQuery(QUERY)
      .map((rs, ctx) ->
        new DMapTarget(
          rs.getString("resource_id"),
          rs.getString("region"),
          rs.getString("subnet_id"),
          rs.getString("private_ip_address"),
          List.of(rs.getString("security_group").split(","))))
      .list());

    LOGGER.debug("Retrieved EC2 assets from DB: {}", scanTargets);
    LOGGER.info("Total EC2 assets to scan: {}", scanTargets.size());

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
