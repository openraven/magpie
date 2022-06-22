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
package io.openraven.magpie.data.aws.ssm;

import io.openraven.magpie.data.aws.AWSResource;
import io.openraven.magpie.data.aws.shared.PayloadUtils;
import software.amazon.awssdk.services.ssm.model.InstanceInformation;

@javax.persistence.Entity
@javax.persistence.Table(name = SSMInstance.TABLE_NAME)
public class SSMInstance extends AWSResource {

  protected static final String TABLE_NAME = "awsssminstance";
    public static final String RESOURCE_TYPE = "AWS::SSM::Instance";

    public SSMInstance() {
        this.resourceType = RESOURCE_TYPE;
    }

    public SSMInstance(String account, String regionId, InstanceInformation instanceInformation) {
        this.awsRegion = regionId;
        this.awsAccountId = account;
        this.arn = String.format("arn:aws:ec2:%s:instance/%s", regionId, instanceInformation.instanceId());
        this.resourceName = instanceInformation.name();
        this.resourceId = instanceInformation.instanceId();
        this.configuration = PayloadUtils.update(instanceInformation);
        this.resourceType = RESOURCE_TYPE;
    }
}
