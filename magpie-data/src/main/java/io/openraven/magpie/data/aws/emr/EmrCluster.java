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
package io.openraven.magpie.data.aws.emr;

import io.openraven.magpie.data.aws.AWSResource;
import io.openraven.magpie.data.aws.shared.PayloadUtils;
import software.amazon.awssdk.services.emr.model.ClusterSummary;

@jakarta.persistence.Entity
@jakarta.persistence.Inheritance(strategy = jakarta.persistence.InheritanceType.TABLE_PER_CLASS)
@jakarta.persistence.Table(name = EmrCluster.TABLE_NAME)
public class EmrCluster extends AWSResource {

  protected static final String TABLE_NAME = "awsemrcluster";
  public static final String RESOURCE_TYPE = "AWS::EMR::Cluster";

  public EmrCluster() {
    this.resourceType = RESOURCE_TYPE;
  }

  public EmrCluster(String account, String regionId, ClusterSummary cluster) {
    this.awsRegion = regionId;
    this.awsAccountId = account;
    this.arn = String.format("arn:aws:emr:::cluster/%s", cluster.id());
    this.resourceId = cluster.id();
    this.resourceName = cluster.name();
    this.configuration = PayloadUtils.update(cluster);
    this.resourceType = RESOURCE_TYPE;
    this.createdIso = cluster.status().timeline().creationDateTime();
  }
}
