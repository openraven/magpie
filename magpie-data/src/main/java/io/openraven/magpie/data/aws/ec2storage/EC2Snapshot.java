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
package io.openraven.magpie.data.aws.ec2storage;

import io.openraven.magpie.data.aws.AWSResource;
import io.openraven.magpie.data.aws.shared.PayloadUtils;
import software.amazon.awssdk.services.ec2.model.Snapshot;

import static java.lang.String.format;

@javax.persistence.Entity
@javax.persistence.Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
@javax.persistence.Table(name = EC2Snapshot.TABLE_NAME)
public class EC2Snapshot extends AWSResource {

  protected static final String TABLE_NAME = "awsec2snapshot";
  public static final String RESOURCE_TYPE = "AWS::EC2::Snapshot";

  @SuppressWarnings("unused")
  public EC2Snapshot() {
    this.resourceType = RESOURCE_TYPE;
  }


  public EC2Snapshot(String accountId, String region, Snapshot snapshot) {
    this.arn = format("arn:aws:ec2:%s:%s:snapshot/%s", region, accountId, snapshot.snapshotId());
    this.awsAccountId = accountId;
    this.awsRegion = region;
    this.resourceId = snapshot.snapshotId();
    this.resourceType = EC2Snapshot.RESOURCE_TYPE;
    this.configuration = PayloadUtils.update(snapshot);
  }
}
