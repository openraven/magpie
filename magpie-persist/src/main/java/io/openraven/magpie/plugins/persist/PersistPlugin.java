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

package io.openraven.magpie.plugins.persist;

import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.api.TerminalPlugin;
import io.openraven.magpie.plugins.persist.mapper.AssetMapper;
import org.slf4j.Logger;

public class PersistPlugin implements TerminalPlugin<PersistConfig> {

  private final Object SYNC = new Object();
  private final AssetMapper MAPPER = new AssetMapper();

  public static final String ID = "magpie.persist";

  private Logger logger;

  private AssetsRepo assetsRepo;

  @Override
  public void accept(MagpieEnvelope env) {
    synchronized (SYNC) {
      AssetModel asset = MAPPER.map(env);
      assetsRepo.upsert(asset);
      logger.debug("Saved asset with id: {}", asset.getAssetId());
    }
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public void init(PersistConfig config, Logger logger) {
    this.logger = logger;
    assetsRepo = new AssetsRepo(config);
    FlywayMigrationService.initiateDBMigration(config);
  }

  @Override
  public void shutdown() {
    synchronized (SYNC) {
    }
  }

  @Override
  public Class<PersistConfig> configType() {
    return PersistConfig.class;
  }
}
