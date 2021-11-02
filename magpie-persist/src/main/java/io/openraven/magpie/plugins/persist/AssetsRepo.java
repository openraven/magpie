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

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;

import javax.persistence.EntityManager;
import java.util.Properties;

import static java.lang.String.format;

public class AssetsRepo {

  private static EntityManager entityManager;

  public AssetsRepo(PersistConfig config) {

    Configuration configuration = new Configuration();

    Properties settings = new Properties();
    settings.put(Environment.DRIVER, "org.postgresql.Driver");
    settings.put(Environment.URL, format("jdbc:postgresql://%s:%s/%s?stringtype=unspecified",
      config.getHostname(), config.getPort(), config.getDatabaseName()));
    settings.put(Environment.USER, config.getUser());
    settings.put(Environment.PASS, config.getPassword());
    settings.put(Environment.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");
    settings.put(Environment.SHOW_SQL, "true");
    settings.put("stringtype", "unspecified");
    settings.put(Environment.HBM2DDL_AUTO, "update");

    configuration.setProperties(settings);

    configuration.addAnnotatedClass(AssetModel.class);

    ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
      .applySettings(configuration.getProperties()).build();

    entityManager = configuration.buildSessionFactory(serviceRegistry).createEntityManager();
  }

  public void upsert(AssetModel assetModel) {
    entityManager.getTransaction().begin();

    entityManager.createQuery("delete from AssetModel where assetId = :id AND resourceType = :resourceType AND accountId = :accountId")
      .setParameter("id", assetModel.getAssetId())
      .setParameter("resourceType", assetModel.getResourceType())
      .setParameter("accountId", assetModel.getAccountId())
      .executeUpdate();

    entityManager.persist(assetModel);

    entityManager.flush();
    entityManager.getTransaction().commit();
    entityManager.clear();
  }
}
