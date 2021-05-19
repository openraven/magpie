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
import org.slf4j.Logger;

import java.time.Instant;

public class PersistPlugin implements TerminalPlugin<PersistConfig> {

  private final Object SYNC = new Object();

  public static final String ID = "magpie.persist";

  private Logger logger;

  AWSResourceRepo awsResourceRepo;

  @Override
  public void accept(MagpieEnvelope env) {
    synchronized (SYNC) {
      var contents = env.getContents();
      String tableName = contents.get("resourceType")
        .asText()
        .replace(":", "")
        .toLowerCase();

      if(!awsResourceRepo.doesTableExist(tableName)) {
        awsResourceRepo.createTable(tableName);
      }

      awsResourceRepo.upsert(tableName,
        contents.get("documentId").asText(),
        contents.get("arn").asText(),
        contents.get("resourceName").asText(),
        contents.get("resourceId").asText(),
        contents.get("resourceType").asText(),
        contents.get("awsRegion").asText(),
        contents.get("createdIso").isNull() ? null : Instant.parse(contents.get("createdIso").textValue()),
        contents.get("updatedIso").isNull() ? null : Instant.parse(contents.get("updatedIso").textValue()),
        contents.get("discoverySessionId").asText(),
        contents.get("maxSizeInBytes").asLong(),
        contents.get("sizeInBytes").asLong(),
        contents.get("configuration").toPrettyString(),
        contents.get("supplementaryConfiguration").toPrettyString(),
        contents.get("tags").toPrettyString(),
        contents.get("discoveryMeta").toPrettyString());

      logger.info("Saved resource with arn: {} into table {}", contents.get("arn"), tableName);
    }
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public void init(PersistConfig config, Logger logger) {
    this.logger = logger;

    awsResourceRepo = new AWSResourceRepo(config);
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
