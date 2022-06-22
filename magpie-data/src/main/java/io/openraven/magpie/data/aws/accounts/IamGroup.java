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
package io.openraven.magpie.data.aws.accounts;

import io.openraven.magpie.data.aws.AWSResource;
import io.openraven.magpie.data.aws.shared.PayloadUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.model.Group;

@javax.persistence.Entity
@javax.persistence.Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
@javax.persistence.Table(name = IamGroup.TABLE_NAME)
public class IamGroup extends AWSResource {

  public static final String TABLE_NAME = "awsiamgroup";
  public static final String RESOURCE_TYPE = "AWS::IAM::Group";

  public IamGroup() {
    this.resourceType = RESOURCE_TYPE;

  }

  public IamGroup(String accountId, String region, Group group) {
    this.arn = group.arn();
    this.awsAccountId = accountId;
    this.awsRegion = Region.AWS_GLOBAL.id();
    this.resourceId = group.groupId();
    this.resourceName = group.groupName();
    this.resourceType = IamGroup.RESOURCE_TYPE;
    this.createdIso = group.createDate();
    this.configuration = PayloadUtils.update(group);
  }
}
