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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.api.TerminalPlugin;
import io.openraven.magpie.data.aws.AWSResource;
import org.slf4j.Logger;

public class PersistPlugin implements TerminalPlugin<PersistConfig> {

  private final Object SYNC = new Object();
  private final ObjectMapper objectMapper = new ObjectMapper()
    .registerModule(new JavaTimeModule());

  public static final String ID = "magpie.persist";

  private Logger logger;

  private AssetsRepo assetsRepo;

  @Override
  public void accept(MagpieEnvelope env) {
    synchronized (SYNC) {
      try {
        AWSResource asset = objectMapper.treeToValue(env.getContents(), AWSResource.class);
        assetsRepo.upsert(asset);
        logger.info("Saved asset with id: {}", asset.arn);
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException(String.format("Unable to parse envelope: %s", env));
      }
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
