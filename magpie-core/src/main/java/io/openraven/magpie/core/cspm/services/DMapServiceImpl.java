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
import io.openraven.magpie.core.cspm.DMapTarget;
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
  private static final String QUERY_TARGETS_SQL =
    "SELECT t.resource_id, t.configuration ->> 'subnetId' as subnet_id, arr.group as security_group " +
    "FROM   assets t, LATERAL (" +
    "   SELECT string_agg(value::jsonb ->> 'groupId', ',') as group " +
    "   FROM   jsonb_array_elements_text(t.configuration->'securityGroups') " +
    "   ) arr " +
    "WHERE t.resource_type = 'AWS::EC2::Instance'";

  private final Jdbi jdbi;

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
    List<DMapTarget> scanTargets = jdbi.withHandle(handle -> handle.createQuery(QUERY_TARGETS_SQL)
      .map((rs, ctx) ->
        new DMapTarget(
          rs.getString("resource_id"),
          rs.getString("subnet_id"),
          List.of(rs.getString("security_group").split(","))))
      .list());

    return scanTargets
      .stream()
      .collect(groupingBy(
        dmapTarget -> new VpcConfig(dmapTarget.getSubnetId(), dmapTarget.getSecurityGroups()),
        mapping(DMapTarget::getResourceId, toList())
      ));
  }
}
