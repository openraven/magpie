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
package io.openraven.magpie.data.aws.s3objects;

import io.openraven.magpie.data.aws.AWSResource;
import io.openraven.magpie.data.aws.shared.PayloadUtils;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.Map;

@javax.persistence.Entity
@javax.persistence.Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
@javax.persistence.Table(name = S3BucketObject.TABLE_NAME)
public class S3BucketObject extends AWSResource {

  protected static final String TABLE_NAME = "awss3bucketobject";
  public static final String RESOURCE_TYPE = "AWS::S3::BucketObject";
  public static final String BUCKET_NAME_KEY = "BucketName";

  @SuppressWarnings("unused")
  public S3BucketObject() {
    this.resourceType = RESOURCE_TYPE;
  }

  public S3BucketObject(String account, String region, String bucketName, S3Object s3Object) {
    awsAccountId = account;
    awsRegion = region;
    final String objectKey = s3Object.key();
    final String objectLocationWithinBucket = String.format("%s/%s", bucketName, objectKey);
    arn = "arn:aws:s3:::" + objectLocationWithinBucket;
    resourceName = objectKey;
    resourceId = objectLocationWithinBucket;
    configuration = PayloadUtils.update(s3Object);
    resourceType = RESOURCE_TYPE;
    createdIso = s3Object.lastModified();
    supplementaryConfiguration = PayloadUtils.update(supplementaryConfiguration,
            Map.of(BUCKET_NAME_KEY, bucketName));
  }

}
