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
package io.openraven.magpie.data.aws.ec2;

import io.openraven.magpie.data.aws.AWSResource;
import io.openraven.magpie.data.aws.shared.PayloadUtils;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;

@javax.persistence.Entity
@javax.persistence.Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
@javax.persistence.Table(name = Ec2Instance.TABLE_NAME)
public class Ec2Instance extends AWSResource {

  protected static final String TABLE_NAME = "awsec2instance";
  public static final String RESOURCE_TYPE = "AWS::EC2::Instance";

  @SuppressWarnings("unused")
  public Ec2Instance() {
    this.resourceType = RESOURCE_TYPE;
  }

  public Ec2Instance(String accountId, String region, Instance instance, Reservation reservation) {
    this.resourceName = instance.instanceId();
    this.resourceId = instance.instanceId();
    this.awsRegion = region;
    this.awsAccountId = accountId;
    this.resourceType = RESOURCE_TYPE;
    this.arn = String
        .format("arn:aws:ec2:%s:%s:instance/%s", region, reservation.ownerId(), instance.instanceId());
    this.configuration = PayloadUtils.update(instance);
    this.createdIso = instance.launchTime();
  }

}
