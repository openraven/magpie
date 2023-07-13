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

import com.fasterxml.jackson.databind.node.NullNode;
import io.openraven.magpie.data.aws.AWSResource;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionMetadata;

import static java.lang.String.format;

@jakarta.persistence.Entity
@jakarta.persistence.Inheritance(strategy = jakarta.persistence.InheritanceType.TABLE_PER_CLASS)
@jakarta.persistence.Table(name = RegionResource.TABLE_NAME)
public class RegionResource extends AWSResource {

  protected static final String TABLE_NAME = "awsregion";
  public static final String RESOURCE_TYPE = "AWS::Region";

  @SuppressWarnings("unused")
  public RegionResource() {
    this.resourceType = RESOURCE_TYPE;
  }


  public RegionResource(String accountId, Region region) {
    this.arn = format("arn:aws::%s:%s", region.id(), accountId);
    this.awsAccountId = accountId;
    this.awsRegion = region.id();
    this.resourceId = region.id();
    final RegionMetadata metadata = region.metadata();
    if (metadata != null) {
      this.resourceName = metadata.description();
    }
    this.resourceType = RegionResource.RESOURCE_TYPE;
    this.configuration = NullNode.instance;
  }
}
