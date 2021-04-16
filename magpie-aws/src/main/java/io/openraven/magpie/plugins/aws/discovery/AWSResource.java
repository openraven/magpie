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

package io.openraven.magpie.plugins.aws.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AWSResource {
  public String arn;
  public String resourceName;
  public String resourceId;
  public String resourceType;
  public String awsRegion;
  public String awsAccountId;
  public String createdIso;
  public String updatedIso;
  public String discoverySessionId;
  public Long maxSizeInBytes = null;
  public Long sizeInBytes = null;

  public JsonNode configuration;
  public JsonNode supplementaryConfiguration;
  public JsonNode tags;
  public JsonNode discoveryMeta;

  private AWSResource() {}

  public AWSResource(Object configuration, String region, String account, ObjectMapper mapper) {
    this.configuration = mapper.valueToTree(configuration);
    this.supplementaryConfiguration = mapper.createObjectNode();
    this.tags = mapper.createObjectNode();
    this.discoveryMeta = mapper.createObjectNode();

    this.awsRegion = region;
    this.awsAccountId = account;
  }

  public ObjectNode toJsonNode(ObjectMapper mapper) {
    var data = mapper.createObjectNode();

    data.put("arn", arn);
    data.put("resourceName", resourceName);
    data.put("resourceId", resourceId);
    data.put("resourceType", resourceType);
    data.put("awsRegion", awsRegion);
    data.put("awsAccountId", awsAccountId);
    data.put("createdIso", createdIso);
    data.put("updatedIso", updatedIso);
    data.put("discoverySessionId", discoverySessionId);
    data.put("maxSizeInBytes", maxSizeInBytes);
    data.put("sizeInBytes", sizeInBytes);

    data.set("configuration", configuration);
    data.set("supplementaryConfiguration", supplementaryConfiguration);
    data.set("tags", tags);
    data.set("discoveryMeta", discoveryMeta);

    return data;
  }
}

