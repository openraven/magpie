/*-
 * #%L
 * Magpie API
 * %%
 * Copyright (C) 2021 Open Raven Inc
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
package io.openraven.magpie.data.aws.elasticache;

import io.openraven.magpie.data.aws.AWSResource;
import io.openraven.magpie.data.aws.shared.PayloadUtils;
import software.amazon.awssdk.services.elasticache.model.CacheCluster;

@javax.persistence.Entity
@javax.persistence.Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
@javax.persistence.Table(name = ElastiCacheCluster.TABLE_NAME)
public class ElastiCacheCluster extends AWSResource {

  protected static final String TABLE_NAME = "awselasticachecluster";
  public static final String RESOURCE_TYPE = "AWS::ElastiCache::Cluster";

  public ElastiCacheCluster() {
    this.resourceType = RESOURCE_TYPE;

  }


  public ElastiCacheCluster(String account, String regionId, CacheCluster cluster) {
    this.awsRegion = regionId;
    this.awsAccountId = account;
    this.arn = cluster.arn();
    this.resourceId = cluster.cacheClusterId();
    this.resourceName = cluster.cacheClusterId();
    this.configuration = PayloadUtils.update(cluster);
    this.resourceType = RESOURCE_TYPE;
  }
}
