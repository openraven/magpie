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

package io.openraven.magpie.core.cspm.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.core.config.ConfigException;
import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.core.cspm.ScanTarget;
import io.openraven.magpie.core.cspm.VpcConfig;
import io.openraven.magpie.plugins.persist.PersistConfig;
import io.openraven.magpie.plugins.persist.PersistPlugin;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.*;

public class DMapServiceImpl implements DMapService {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private Jdbi jdbi;
  private static final String QUERY_TARGETS_SQL =
    "SELECT t.resource_id, " +
    "       t.configuration ->> 'subnetId' as subnetId, " +
    "       arr.security_groups ->> 'groupId' as securityGroup " +
    "FROM assets t, LATERAL (" +
    "   SELECT value::jsonb AS security_groups" +
    "   FROM   jsonb_array_elements_text(t.configuration->'securityGroups')" +
    "   ) arr" +
    "where t.resource_type = 'AWS::EC2::Instance'";

  public DMapServiceImpl(MagpieConfig config) {
    final var rawPersistConfig = config.getPlugins().get(PersistPlugin.ID);
    if (rawPersistConfig == null) {
      throw new ConfigException(String.format("Config file does not contain %s configuration", PersistPlugin.ID));
    }

    try {
      final PersistConfig persistConfig = MAPPER.treeToValue(MAPPER.valueToTree(rawPersistConfig.getConfig()), PersistConfig.class);

      String url = String.format("jdbc:postgresql://%s:%s/%s", persistConfig.getHostname(), persistConfig.getPort(), persistConfig.getDatabaseName());
      jdbi = Jdbi.create(url, persistConfig.getUser(), persistConfig.getPassword())
        .installPlugin(new PostgresPlugin());
    } catch (JsonProcessingException e) {
      throw new ConfigException("Cannot instantiate PersistConfig while initializing PolicyAnalyzerService", e);
    }
  }

  @Override
  public Map<VpcConfig, List<String>> groupScanTargets() {
    List<ScanTarget> scanTargets = jdbi.withHandle(handle -> handle.createQuery(QUERY_TARGETS_SQL)
      .map((rs, ctx) ->
        new ScanTarget(
          rs.getString("resource_id"),
          rs.getString("subnetId"),
          List.of(rs.getString("securityGroup").split(","))))
      .list());

    return scanTargets
      .stream()
      .collect(groupingBy(
        scanTarget -> new VpcConfig(scanTarget.getSubnetId(), scanTarget.getSecurityGroups()),
        mapping(ScanTarget::getResourceId, toList())
      ));
  }
}
