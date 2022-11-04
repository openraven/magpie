/*-
 * #%L
 * Magpie API
 * %%
 * Copyright (C) 2021 - 2022 Open Raven Inc
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
package io.openraven.magpie.data.aws.rds;

import io.openraven.magpie.data.aws.AWSResource;
import io.openraven.magpie.data.aws.shared.PayloadUtils;
import software.amazon.awssdk.services.rds.model.DBProxy;


@javax.persistence.Entity
@javax.persistence.Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
@javax.persistence.Table(name = RDSProxy.TABLE_NAME)
public class RDSProxy extends AWSResource {

  protected static final String TABLE_NAME = "awsrdsproxy";
  public static final String RESOURCE_TYPE = "AWS::RDS::DBProxy";

  public RDSProxy() {
    this.resourceType = RESOURCE_TYPE;
  }

  public RDSProxy(String account, String regionId, DBProxy dbProxy) {
    this.awsRegion = regionId;
    this.awsAccountId = account;
    this.arn = dbProxy.dbProxyArn();
    this.resourceId = dbProxy.dbProxyArn();
    this.resourceName = dbProxy.dbProxyName();
    this.resourceType = RESOURCE_TYPE;
    this.configuration = PayloadUtils.update(dbProxy);
    this.createdIso = dbProxy.createdDate();
  }

}
