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
package io.openraven.magpie.data.aws.fsx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.data.aws.AWSResource;
import io.openraven.magpie.data.aws.shared.PayloadUtils;
import software.amazon.awssdk.services.fsx.model.FileSystem;
import software.amazon.awssdk.services.fsx.model.Tag;

import java.util.stream.Collectors;

@javax.persistence.Entity
@javax.persistence.Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
@javax.persistence.Table(name = FSxFileSystem.TABLE_NAME)
public class FSxFileSystem extends AWSResource {

  protected static final String TABLE_NAME = "awsfsxfilesystem";
  public static final String RESOURCE_TYPE = "AWS::FSx::FileSystem";

  @SuppressWarnings("unused")
  public FSxFileSystem() {
    this.resourceType = RESOURCE_TYPE;
  }

  @SuppressWarnings("unused")
  public FSxFileSystem(String account, String regionId, FileSystem fileSystem) {
    this.awsRegion = regionId;
    this.awsAccountId = account;
    this.arn = fileSystem.resourceARN();
    this.resourceId = fileSystem.fileSystemId();
    this.resourceName = fileSystem.fileSystemId();
    this.configuration = PayloadUtils.update(fileSystem);
    this.resourceType = RESOURCE_TYPE;
    this.createdIso = fileSystem.creationTime();

  }

  public FSxFileSystem(String account, String regionId, FileSystem fileSystem, ObjectMapper objectMapper) {
    this(account, regionId, fileSystem);
    this.tags = objectMapper.convertValue(
            fileSystem.tags().stream()
                    .collect(Collectors.toMap(Tag::key, Tag::value)),
            JsonNode.class
    );
  }
}
