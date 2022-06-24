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
import software.amazon.awssdk.services.ec2.model.NetworkAcl;

import static java.lang.String.format;

@javax.persistence.Entity
@javax.persistence.Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
@javax.persistence.Table(name = Ec2NetworkAcl.TABLE_NAME)
public class Ec2NetworkAcl extends AWSResource {

  protected static final String TABLE_NAME = "awsec2networkacl";
  public static final String RESOURCE_TYPE = "AWS::EC2::NetworkAcl";

  @SuppressWarnings("unused")
  public Ec2NetworkAcl() {
    this.resourceType = RESOURCE_TYPE;
  }

  public Ec2NetworkAcl(String accountId, String region, NetworkAcl networkAcl) {
    this.arn = format("arn:aws:ec2:%s:%s:network-acl/%s", region, accountId,
            networkAcl.networkAclId());
    this.awsAccountId = accountId;
    this.awsRegion = region;
    this.resourceId = networkAcl.networkAclId();
    this.resourceName = networkAcl.networkAclId();
    this.resourceType = RESOURCE_TYPE;
    this.configuration = PayloadUtils.update(networkAcl);
  }
}
