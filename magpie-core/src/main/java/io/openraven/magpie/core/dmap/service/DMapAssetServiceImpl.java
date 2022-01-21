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
import io.openraven.magpie.plugins.persist.AssetsRepo;
import io.openraven.magpie.plugins.persist.PersistConfig;
import io.openraven.magpie.plugins.persist.PersistPlugin;
import io.openraven.magpie.plugins.persist.impl.HibernateAssetsRepoImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.*;

public class DMapAssetServiceImpl implements DMapAssetService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DMapAssetServiceImpl.class);
  private static final String QUERY = Util.getResourceAsString("/sql/dmap-asset-grouping.sql");

  private final ObjectMapper mapper = new ObjectMapper();
  private AssetsRepo assetsRepo;

  public DMapAssetServiceImpl(MagpieConfig config) {
    this.assetsRepo = initPersistence(config);
  }

  @Override
  public Map<VpcConfig, List<EC2Target>> groupScanTargets() {
    List<Map<String, Object>> maps = assetsRepo.queryNative(QUERY);

    List<DMapTarget> scanTargets = maps
      .stream()
      .map(tuple -> new DMapTarget(
        tuple.get("resourceid").toString(),
        String.valueOf(tuple.get("region")), // TODO rewrite
        tuple.get("subnet_id").toString(),
        tuple.get("private_ip_address").toString(),
        List.of(tuple.get("security_group").toString().split(","))))
      .collect(toList());

    LOGGER.debug("Retrieved EC2 assets from DB: {}", scanTargets);
    LOGGER.info("Total EC2 assets to scan: {}", scanTargets.size());

    return scanTargets
      .stream()
      .collect(groupingBy(
        dmapTarget -> new VpcConfig(dmapTarget.getRegion(), dmapTarget.getSubnetId(), dmapTarget.getSecurityGroups()),
        mapping(dmapTarget -> new EC2Target(dmapTarget.getResourceId(), dmapTarget.getPrivateIpAddress()), toList())
      ));
  }

  private AssetsRepo initPersistence(MagpieConfig config) {
    final var rawPersistConfig = config.getPlugins().get(PersistPlugin.ID);
    if (rawPersistConfig == null) {
      throw new ConfigException(String.format("Config file does not contain %s configuration", PersistPlugin.ID));
    }

    try {
      final PersistConfig persistConfig = mapper.treeToValue(mapper.valueToTree(rawPersistConfig.getConfig()), PersistConfig.class);
      return new HibernateAssetsRepoImpl(persistConfig);
    } catch (JsonProcessingException e) {
      throw new ConfigException("Cannot instantiate PersistConfig while initializing PolicyAnalyzerService", e);
    }
  }
}
