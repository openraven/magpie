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

package io.openraven.magpie.plugins.persist.config;

import io.openraven.magpie.data.Resource;
import io.openraven.magpie.plugins.persist.PersistConfig;
import io.openraven.magpie.plugins.persist.migration.FlywayMigrationService;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.Properties;

import static io.openraven.magpie.data.utils.EntityTypeResolver.getSubClasses;
import static java.lang.String.format;

public class PostgresPersistenceProvider {

  public static EntityManager getEntityManager(PersistConfig config) {

    Properties settings = new Properties();
    settings.put(Environment.DRIVER, "org.postgresql.Driver");
    settings.put(Environment.URL, format("jdbc:postgresql://%s:%s/%s?stringtype=unspecified",
      config.getHostname(), config.getPort(), config.getDatabaseName()));
    settings.put(Environment.USER, config.getUser());
    settings.put(Environment.PASS, config.getPassword());
    settings.put(Environment.DIALECT, "io.openraven.magpie.plugins.persist.config.PostgreSQL10StringDialect");
    settings.put(Environment.SHOW_SQL, "false");
    settings.put(Environment.HBM2DDL_AUTO, Optional.ofNullable(config.getDdlAuto()).orElse("validate"));
    settings.put(Environment.DEFAULT_SCHEMA, config.getSchema());

    Configuration configuration = new Configuration();
    configuration.setProperties(settings);

    getSubClasses(Resource.class).forEach(configuration::addAnnotatedClass);

    ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
      .applySettings(configuration.getProperties()).build();
    if(config.shouldMigrateDB()) {
        migratePostgreDB(config); // migrating DB before EM creation to validate schema further
    }

    return configuration.buildSessionFactory(serviceRegistry).createEntityManager();
  }

  private static void migratePostgreDB(PersistConfig config) {
    FlywayMigrationService.initiateDBMigration(config);
  }

}
