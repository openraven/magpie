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
import software.amazon.awssdk.services.ec2.model.SecurityGroup;

import static java.lang.String.format;

@javax.persistence.Entity
@javax.persistence.Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
@javax.persistence.Table(name = EC2SecurityGroup.TABLE_NAME)
public class EC2SecurityGroup extends AWSResource {

  protected static final String TABLE_NAME = "awsec2securitygroup";
  public static final String RESOURCE_TYPE = "AWS::EC2::SecurityGroup";

  @SuppressWarnings("unused")
  public EC2SecurityGroup() {
    this.resourceType = EC2SecurityGroup.RESOURCE_TYPE;
  }

  public EC2SecurityGroup(String accountId, String region, SecurityGroup securityGroup) {
    this.arn = format("arn:aws:ec2:%s:%s:security-group/%s", region, accountId,
            securityGroup.groupId());
    this.awsAccountId = accountId;
    this.awsRegion = region;
    this.resourceId = securityGroup.groupId();
    this.resourceName = securityGroup.groupName();
    this.resourceType = EC2SecurityGroup.RESOURCE_TYPE;
    this.configuration = PayloadUtils.update(securityGroup);
  }

  public static class OwnerCIDR {
    public String cidr;
    public String owner;
    public String ownerNetrange;
    public boolean isBadCidr;

    @SuppressWarnings("unused")
    public OwnerCIDR() {}

    public OwnerCIDR(String cidr, String owner, String ownerNetrange, boolean isBadCidr){
      this.cidr = cidr;
      this.owner = owner;
      this.ownerNetrange = ownerNetrange;
      this.isBadCidr = isBadCidr;
    }
  }

  public static class WhoisLookupException extends Exception {

    public WhoisLookupException(String message) {
      super(message);
    }
  }
}
