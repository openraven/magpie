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

package io.openraven.magpie.plugins.persist.impl;

import io.openraven.magpie.data.Resource;
import io.openraven.magpie.plugins.persist.AssetsRepo;
import io.openraven.magpie.plugins.persist.PersistConfig;
import io.openraven.magpie.plugins.persist.config.PostgresPersistenceProvider;
import jakarta.persistence.EntityManager;
import org.hibernate.query.sql.internal.NativeQueryImpl;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HibernateAssetsRepoImpl implements AssetsRepo, Closeable {
  private final Logger logger = LoggerFactory.getLogger(HibernateAssetsRepoImpl.class);

  private final EntityManager entityManager;
  private final PersistConfig persistConfig;

  public HibernateAssetsRepoImpl(PersistConfig persistConfig) {
    this.entityManager = PostgresPersistenceProvider.getEntityManager(persistConfig);
    this.persistConfig = persistConfig;
  }

  public void upsert(Resource resource) {
    try {
      entityManager.getTransaction().begin();

      entityManager.merge(resource);

      entityManager.flush();
      entityManager.getTransaction().commit();
      entityManager.clear();
    } catch (Exception e) {
      logger.error("Rolling back transaction failed due to: " + e.getMessage());
      logger.debug("Details", e);
      entityManager.getTransaction().rollback();
    }
  }

  public void upsert(List<Resource> resources) {
    try {
      entityManager.getTransaction().begin();

      resources.forEach(entityManager::merge);

      entityManager.flush();
      entityManager.getTransaction().commit();
      entityManager.clear();
    } catch (Exception e) {
      logger.error("Rolling back transaction failed due to: " + e.getMessage());
      logger.debug("Details", e);
      entityManager.getTransaction().rollback();
    }
  }

  @Override
  public void executeNative(String query) {
    try {
      entityManager.getTransaction().begin();

      entityManager.createNativeQuery(query).executeUpdate();

      entityManager.flush();
      entityManager.getTransaction().commit();
      entityManager.clear();
    } catch (Exception e) {
      logger.error("Rolling back transaction failed due to: " + e.getMessage());
      logger.debug("Details", e);
      entityManager.getTransaction().rollback();
      throw(e);
    }
  }

  @Override
  public List<Map<String, Object>> queryNative(String query) {
    return entityManager.createNativeQuery(query)
      .unwrap(NativeQueryImpl.class)
      .setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE)
      .getResultList();
  }

  @Override
  public Long getAssetCount(String resourceType) {

    // Sadly, table names cannot be parameterized in queries, and simple string substitution opens us up to SQL
    // injection. So we must account for both AWS and GCP in our logic.
    final var provider = resourceType.split(":")[0].toLowerCase(Locale.ROOT);
    final var query = "aws".equals(provider) ?
      "SELECT COUNT(*) FROM " + persistConfig.getSchema() + ".aws WHERE resourcetype = :resourceType":
      "SELECT COUNT(*) FROM "  + persistConfig.getSchema() + ".gcp WHERE resourcetype = :resourceType";

    BigInteger val = (BigInteger)entityManager.createNativeQuery(query)
      .setParameter("resourceType", resourceType)
      .getResultList()
      .get(0);

    return val.longValue();
  }


  @Override
  public void close() throws IOException {
    entityManager.close();
  }
}
