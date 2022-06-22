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
package io.openraven.magpie.data.aws.secretsmanager;

import io.openraven.magpie.data.aws.AWSResource;

@javax.persistence.Entity
@javax.persistence.Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
@javax.persistence.Table(name = SecretsManagerSecret.TABLE_NAME)
public class SecretsManagerSecret extends AWSResource {

  protected static final String TABLE_NAME = "awssecretsmanager";
    public static final String RESOURCE_TYPE = "AWS::SecretsManager";

    public SecretsManagerSecret() {
        this.resourceType = RESOURCE_TYPE;
    }

    public SecretsManagerSecret(String account, String regionId) {
        this.awsRegion = regionId;
        this.awsAccountId = account;
        this.arn = RESOURCE_TYPE;
        this.resourceName = RESOURCE_TYPE;
        this.resourceId = RESOURCE_TYPE;
        this.resourceType = RESOURCE_TYPE;
    }
}
