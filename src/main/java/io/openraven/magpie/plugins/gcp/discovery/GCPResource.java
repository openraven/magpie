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

package io.openraven.magpie.plugins.gcp.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;

public class GCPResource {
  private final ObjectMapper mapper = GCPUtils.createObjectMapper();

  public String projectId;
  public String resourceName;
  public String resourceType;
  public String region;
  public String accountId;
  public Instant createdIso;
  public Instant updatedIso;
  public String discoverySessionId;
  public Long maxSizeInBytes = null;
  public Long sizeInBytes = null;

  public JsonNode configuration;
  public JsonNode supplementaryConfiguration;
  public JsonNode tags;
  public JsonNode discoveryMeta;

  public GCPResource(String name, String projectId, String resourceType) {
    this.resourceName = name;
    this.projectId = projectId;
    this.resourceType = resourceType;

    this.configuration = mapper.createObjectNode();
    this.supplementaryConfiguration = mapper.createObjectNode();
    this.tags = mapper.createObjectNode();
    this.discoveryMeta = mapper.createObjectNode();
  }

  public ObjectNode toJsonNode() {
    var data = mapper.createObjectNode();

    data.put("documentId", EncodedNamedUUIDGenerator.getEncodedNamedUUID(resourceName));

    data.put("projectId", projectId);
    data.put("resourceName", resourceName);
    data.put("resourceType", resourceType);
    data.put("region", region);
    data.put("accountId", accountId);
    data.put("createdIso", createdIso == null ? null : createdIso.toString());
    data.put("updatedIso", updatedIso == null ? null : updatedIso.toString());
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

