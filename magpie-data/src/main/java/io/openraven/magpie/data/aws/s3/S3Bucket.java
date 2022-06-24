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
package io.openraven.magpie.data.aws.s3;

import io.openraven.magpie.data.aws.AWSResource;
import io.openraven.magpie.data.aws.shared.PayloadUtils;
import software.amazon.awssdk.services.s3.model.Bucket;

@javax.persistence.Entity
@javax.persistence.Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
@javax.persistence.Table(name = S3Bucket.TABLE_NAME)
public class S3Bucket extends AWSResource {

  protected static final String TABLE_NAME = "awss3bucket";
  public static final String RESOURCE_TYPE = "AWS::S3::Bucket";

  @SuppressWarnings("unused")
  public S3Bucket() {
    this.resourceType = RESOURCE_TYPE;
  }

  public S3Bucket(String account, String region, Bucket bucket) {
    awsAccountId = account;
    awsRegion = region;
    arn = "arn:aws:s3:::" + bucket.name();
    resourceName = bucket.name();
    resourceId = bucket.name();
    configuration = PayloadUtils.update(bucket);
    resourceType = RESOURCE_TYPE;
    createdIso = bucket.creationDate();
  }

}
