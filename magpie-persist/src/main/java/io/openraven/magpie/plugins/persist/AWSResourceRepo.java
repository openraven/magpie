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

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import java.time.Instant;

import static java.lang.String.format;

public class AWSResourceRepo {
  private final Jdbi jdbi;

  AWSResourceRepo(PersistConfig config) {
    String url = format("jdbc:postgresql://%s:%s/%s", config.getHostname(), config.getPort(), config.getDatabaseName());
    jdbi = Jdbi.create(url, config.getUser(), config.getPassword())
      .installPlugin(new PostgresPlugin())
      .installPlugin(new SqlObjectPlugin());
  }

  boolean doesTableExist(String tableName) {
    return jdbi.withExtension(AWSResourceDao.class, dao -> dao.doesTableExist(tableName).equals("1"));
  }

  void createTable(String tableName) {
    jdbi.useExtension(AWSResourceDao.class, dao -> dao.createTable(tableName));
  }

  void upsert(String tableName,
              String documentId,
              String arn,
              String resourceName,
              String resourceId,
              String resourceType,
              String awsRegion,
              Instant createdIso,
              Instant updatedIso,
              String discoverySessionId,
              long maxSizeInBytes,
              long sizeInBytes,
              String configuration,
              String supplementaryConfiguration,
              String tags,
              String discoveryMeta) {
    jdbi.useExtension(AWSResourceDao.class, dao -> {
      dao.removeRecord(tableName, documentId);

      dao.insert(tableName,
        documentId,
        arn,
        resourceName,
        resourceId,
        resourceType,
        awsRegion,
        createdIso,
        updatedIso,
        discoverySessionId,
        maxSizeInBytes,
        sizeInBytes,
        configuration,
        supplementaryConfiguration,
        tags,
        discoveryMeta);
    });
  }
}
