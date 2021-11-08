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

import io.openraven.magpie.data.aws.AWSResource;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;

import javax.persistence.EntityManager;
import java.util.Properties;

import static io.openraven.magpie.data.aws.utils.AwsEntityTypeResolver.getSubClasses;
import static java.lang.String.format;

public class PostgresPersistenceProvider {

  public static EntityManager getEntityManager(PersistConfig config) {

    Properties settings = new Properties();
    settings.put(Environment.DRIVER, "org.postgresql.Driver");
    settings.put(Environment.URL, format("jdbc:postgresql://%s:%s/%s?stringtype=unspecified",
      config.getHostname(), config.getPort(), config.getDatabaseName()));
    settings.put(Environment.USER, config.getUser());
    settings.put(Environment.PASS, config.getPassword());
    settings.put(Environment.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");
    settings.put(Environment.SHOW_SQL, "true");
    settings.put(Environment.HBM2DDL_AUTO, "update");

    Configuration configuration = new Configuration();
    configuration.setProperties(settings);

    getSubClasses(AWSResource.class).forEach(configuration::addAnnotatedClass);

    ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
      .applySettings(configuration.getProperties()).build();

    return configuration.buildSessionFactory(serviceRegistry).createEntityManager();
  }

}
