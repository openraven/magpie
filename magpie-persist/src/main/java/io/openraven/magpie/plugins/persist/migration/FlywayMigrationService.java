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

package io.openraven.magpie.plugins.persist.migration;

import io.openraven.magpie.plugins.persist.PersistConfig;
import org.flywaydb.core.Flyway;

import static java.lang.String.format;

public class FlywayMigrationService {

  private static final String POSTGRES_URL = "jdbc:postgresql://%s:%s/%s";

  public static void initiateDBMigration(PersistConfig config) {
    String databaseUrl = format(POSTGRES_URL, config.getHostname(), config.getPort(), config.getDatabaseName());
    Flyway
      .configure()
      .dataSource(databaseUrl, config.getUser(), config.getPassword())
      .load()
      .migrate();
  }
}
