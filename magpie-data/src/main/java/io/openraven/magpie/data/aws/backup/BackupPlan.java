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
package io.openraven.magpie.data.aws.backup;

import io.openraven.magpie.data.aws.AWSResource;
import io.openraven.magpie.data.aws.shared.PayloadUtils;
import software.amazon.awssdk.services.backup.model.BackupPlansListMember;

@javax.persistence.Entity
@javax.persistence.Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
@javax.persistence.Table(name = BackupPlan.TABLE_NAME)
public class BackupPlan extends AWSResource {

  protected static final String TABLE_NAME = "awsbackupbackupplan";
  public static final String RESOURCE_TYPE = "AWS::Backup::BackupPlan";

  public BackupPlan() {
    this.resourceType = RESOURCE_TYPE;

  }

  public BackupPlan(String accountId, String region, BackupPlansListMember backupPlanListMember) {
    this.awsAccountId = accountId;
    this.awsRegion = region;
    this.configuration = PayloadUtils.update(backupPlanListMember);
    this.resourceType = RESOURCE_TYPE;
    this.arn = backupPlanListMember.backupPlanArn();
    this.resourceName = backupPlanListMember.backupPlanName();
    this.resourceId = backupPlanListMember.backupPlanId();
    this.createdIso = backupPlanListMember.creationDate();
  }
}
