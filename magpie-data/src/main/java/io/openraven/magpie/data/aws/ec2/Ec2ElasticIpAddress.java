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
import software.amazon.awssdk.services.ec2.model.Address;

import static java.lang.String.format;

@jakarta.persistence.Entity
@jakarta.persistence.Inheritance(strategy = jakarta.persistence.InheritanceType.TABLE_PER_CLASS)
@jakarta.persistence.Table(name = Ec2ElasticIpAddress.TABLE_NAME)
public class Ec2ElasticIpAddress extends AWSResource {

  protected static final String TABLE_NAME = "awsec2eip";
  public static final String RESOURCE_TYPE = "AWS::EC2::EIP";

  @SuppressWarnings("unused")
  public Ec2ElasticIpAddress() {
    this.resourceType = RESOURCE_TYPE;
  }

  public Ec2ElasticIpAddress(String accountId, String region, Address address) {
    this.arn = format("arn:aws:ec2:%s:%s:eip-allocation/%s", region, accountId, address.allocationId());
    this.awsAccountId = accountId;
    this.awsRegion = region;
    this.resourceId = address.allocationId();
    this.resourceName = address.publicIp();
    this.resourceType = Ec2ElasticIpAddress.RESOURCE_TYPE;
    this.configuration = PayloadUtils.update(address);
  }
}
