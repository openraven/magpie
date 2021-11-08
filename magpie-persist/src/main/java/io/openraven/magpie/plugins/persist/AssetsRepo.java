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

import javax.persistence.EntityManager;

public class AssetsRepo {

  private final EntityManager entityManager;

  public AssetsRepo(PersistConfig persistConfig) {
    this.entityManager = PostgresPersistenceProvider.getEntityManager(persistConfig);
  }

  public void upsert(AWSResource awsResource) {
    entityManager.getTransaction().begin();

    AWSResource merge = entityManager.merge(awsResource);

    entityManager.flush();
    entityManager.getTransaction().commit();
    entityManager.clear();
  }
}
