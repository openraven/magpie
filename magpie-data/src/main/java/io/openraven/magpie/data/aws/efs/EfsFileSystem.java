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
package io.openraven.magpie.data.aws.efs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.data.aws.AWSResource;
import io.openraven.magpie.data.aws.shared.PayloadUtils;
import software.amazon.awssdk.services.efs.model.FileSystemDescription;
import software.amazon.awssdk.services.efs.model.Tag;

import java.util.stream.Collectors;

@javax.persistence.Entity
@javax.persistence.Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
@javax.persistence.Table(name = EfsFileSystem.TABLE_NAME)
public class EfsFileSystem extends AWSResource {
  protected static final String TABLE_NAME = "awsefsfilesystem";
  public static final String RESOURCE_TYPE = "AWS::EFS::FileSystem";

  @SuppressWarnings("unused")
  public EfsFileSystem(){
    this.resourceType = RESOURCE_TYPE;
  }

  @SuppressWarnings("unused")
  public EfsFileSystem(String region, FileSystemDescription fileSystem) {
    this.resourceId = fileSystem.fileSystemId();
    this.awsAccountId = fileSystem.ownerId();
    this.awsRegion = region;
    this.arn = String.format("arn:aws:elasticfilesystem:%s:%s:file-system/%s", region, fileSystem.ownerId(),fileSystem.fileSystemId());
    this.resourceName = fileSystem.name();
    this.configuration = PayloadUtils.update(fileSystem);
    this.resourceType = RESOURCE_TYPE;
    this.createdIso = fileSystem.creationTime();

  }

  public EfsFileSystem(String region, FileSystemDescription fileSystem, ObjectMapper objectMapper) {
    this(region, fileSystem);
    this.tags = objectMapper.convertValue(
            fileSystem.tags().stream()
                    .collect(Collectors.toMap(Tag::key, Tag::value)),
            JsonNode.class
    );
  }
}
