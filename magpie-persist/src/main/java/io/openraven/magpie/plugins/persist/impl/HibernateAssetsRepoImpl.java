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

import io.openraven.magpie.data.aws.AWSResource;
import io.openraven.magpie.plugins.persist.AssetModel;
import io.openraven.magpie.plugins.persist.AssetsRepo;
import io.openraven.magpie.plugins.persist.PersistConfig;
import io.openraven.magpie.plugins.persist.config.PostgresPersistenceProvider;
import org.hibernate.query.internal.NativeQueryImpl;
import org.hibernate.transform.AliasToEntityMapResultTransformer;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;

public class HibernateAssetsRepoImpl implements AssetsRepo {

  private final EntityManager entityManager;

  public HibernateAssetsRepoImpl(PersistConfig persistConfig) {
    this.entityManager = PostgresPersistenceProvider.getEntityManager(persistConfig);
  }

  public void upsert(AWSResource awsResource) {
    entityManager.getTransaction().begin();

    entityManager.merge(awsResource);

    entityManager.flush();
    entityManager.getTransaction().commit();
    entityManager.clear();
  }

  // Keeping so far for backward compatibility with rules
  public void upsert(AssetModel assetModel) {
    entityManager.getTransaction().begin();

    entityManager.merge(assetModel);

    entityManager.flush();
    entityManager.getTransaction().commit();
    entityManager.clear();
  }

  @Override
  public void executeNative(String query) {
    entityManager.getTransaction().begin();

    entityManager.createNativeQuery(query).executeUpdate();

    entityManager.flush();
    entityManager.getTransaction().commit();
    entityManager.clear();
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
    return entityManager.createQuery("SELECT COUNT(a) FROM AssetModel a WHERE resourceType = :resourceType", Long.class)
      .setParameter("resourceType", resourceType)
      .getSingleResult();
  }


}
