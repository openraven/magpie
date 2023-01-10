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

package io.openraven.magpie.api;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;

import static io.openraven.magpie.api.utils.EncodedNamedUUIDGenerator.getEncodedNamedUUID;

public class MagpieGdriveResource {
  private final ObjectMapper mapper;

  public String assetId;
  public String resourceName;
  public String drive;
  public String resourceType;
  public String domain;
  public Instant createdIso;
  public Instant updatedIso = Instant.now();
  public String discoverySessionId;

  public Long maxSizeInBytes = null;
  public Long sizeInBytes = null;

  public JsonNode configuration;
  public JsonNode supplementaryConfiguration;
  public JsonNode tags;
  public JsonNode discoveryMeta;

  private MagpieGdriveResource(MagpieGdriveResource.MagpieGdriveResourceBuilder builder) {
    this.assetId = builder.assetId;
    this.resourceName = builder.resourceName;
    this.drive = builder.drive;
    this.resourceType = builder.resourceType;
    this.domain = builder.domain;
    this.createdIso = builder.createdIso;
    this.updatedIso = builder.updatedIso;
    this.discoverySessionId = builder.discoverySessionId;
    this.maxSizeInBytes = builder.maxSizeInBytes;
    this.sizeInBytes = builder.sizeInBytes;
    this.configuration = builder.configuration;
    this.supplementaryConfiguration = builder.supplementaryConfiguration;
    this.tags = builder.tags;
    this.discoveryMeta = builder.discoveryMeta;
    this.mapper = builder.mapper;
  }

  public ObjectNode toJsonNode() {
    var data = mapper.createObjectNode();

    data.put("documentId", getEncodedNamedUUID(assetId + resourceType + domain));
    data.put("assetId", assetId);
    data.put("resourceName", resourceName);
    data.put("drive", drive);
    data.put("resourceType", resourceType);
    data.put("domain", domain);
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

  public String getAssetId() {
    return assetId;
  }

  public void setAssetId(String fileId) {
    this.assetId = assetId;
  }

  public String getResourceName() {
    return resourceName;
  }

  public void setResourceName(String resourceName) {
    this.resourceName = resourceName;
  }

  public String getResourceType() {
    return resourceType;
  }

  public void setResourceType(String resourceType) {
    this.resourceType = resourceType;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }


  public void setDrive(String space) {
    this.drive = drive;
  }

  public String getDrive() {
    return drive;
  }

  public Instant getCreatedIso() {
    return createdIso;
  }

  public void setCreatedIso(Instant createdIso) {
    this.createdIso = createdIso;
  }

  public Instant getUpdatedIso() {
    return updatedIso;
  }

  public void setUpdatedIso(Instant updatedIso) {
    this.updatedIso = updatedIso;
  }

  public String getDiscoverySessionId() {
    return discoverySessionId;
  }

  public void setDiscoverySessionId(String discoverySessionId) {
    this.discoverySessionId = discoverySessionId;
  }

  public Long getMaxSizeInBytes() {
    return maxSizeInBytes;
  }

  public void setMaxSizeInBytes(Long maxSizeInBytes) {
    this.maxSizeInBytes = maxSizeInBytes;
  }

  public Long getSizeInBytes() {
    return sizeInBytes;
  }

  public void setSizeInBytes(Long sizeInBytes) {
    this.sizeInBytes = sizeInBytes;
  }

  public JsonNode getConfiguration() {
    return configuration;
  }

  public void setConfiguration(JsonNode configuration) {
    this.configuration = configuration;
  }

  public JsonNode getSupplementaryConfiguration() {
    return supplementaryConfiguration;
  }

  public void setSupplementaryConfiguration(JsonNode supplementaryConfiguration) {
    this.supplementaryConfiguration = supplementaryConfiguration;
  }

  public JsonNode getTags() {
    return tags;
  }

  public void setTags(JsonNode tags) {
    this.tags = tags;
  }

  public JsonNode getDiscoveryMeta() {
    return discoveryMeta;
  }

  public void setDiscoveryMeta(JsonNode discoveryMeta) {
    this.discoveryMeta = discoveryMeta;
  }

  public static class MagpieGdriveResourceBuilder {
    private final ObjectMapper mapper;
    private String assetId;
    private String resourceName;
    private String resourceId;
    private String resourceType;
    private String drive;
    private String domain;
    private Instant createdIso;
    private Instant updatedIso = Instant.now();
    private String discoverySessionId;
    private Long maxSizeInBytes = null;
    private Long sizeInBytes = null;
    private JsonNode configuration;
    private JsonNode supplementaryConfiguration;
    private JsonNode tags;
    private JsonNode discoveryMeta;

    public MagpieGdriveResourceBuilder(ObjectMapper mapper, String assetId) {
      this.assetId = assetId;
      this.mapper = mapper;

      this.configuration = mapper.createObjectNode();
      this.supplementaryConfiguration = mapper.createObjectNode();
      this.tags = mapper.createObjectNode();
      this.discoveryMeta = mapper.createObjectNode();
    }

    public MagpieGdriveResource.MagpieGdriveResourceBuilder withResourceName(String resourceName) {
      this.resourceName = resourceName;
      return this;
    }

    public MagpieGdriveResource.MagpieGdriveResourceBuilder withResourceId(String resourceId) {
      this.resourceId = resourceId;
      return this;
    }

    public MagpieGdriveResource.MagpieGdriveResourceBuilder withResourceType(String resourceType) {
      this.resourceType = resourceType;
      return this;
    }

    public MagpieGdriveResource.MagpieGdriveResourceBuilder withRegion(String drive) {
      this.drive = drive;
      return this;
    }

    public MagpieGdriveResource.MagpieGdriveResourceBuilder withProjectId(String domain) {
      this.domain = domain;
      return this;
    }

    public MagpieGdriveResource.MagpieGdriveResourceBuilder withCreatedIso(Instant createdIso) {
      this.createdIso = createdIso;
      return this;
    }

    public MagpieGdriveResource.MagpieGdriveResourceBuilder withUpdatedIso(Instant updatedIso) {
      this.updatedIso = updatedIso;
      return this;
    }

    public MagpieGdriveResource.MagpieGdriveResourceBuilder withDiscoverySessionId(String discoverySessionId) {
      this.discoverySessionId = discoverySessionId;
      return this;
    }

    public MagpieGdriveResource.MagpieGdriveResourceBuilder withMaxSizeInBytes(Long maxSizeInBytes) {
      this.maxSizeInBytes = maxSizeInBytes;
      return this;
    }

    public MagpieGdriveResource.MagpieGdriveResourceBuilder withSizeInBytes(Long sizeInBytes) {
      this.sizeInBytes = sizeInBytes;
      return this;
    }

    public MagpieGdriveResource.MagpieGdriveResourceBuilder withConfiguration(JsonNode configuration) {
      this.configuration = configuration;
      return this;
    }

    public MagpieGdriveResource.MagpieGdriveResourceBuilder withSupplementaryConfiguration(JsonNode supplementaryConfiguration) {
      this.supplementaryConfiguration = supplementaryConfiguration;
      return this;
    }

    public MagpieGdriveResource.MagpieGdriveResourceBuilder withTags(JsonNode tags) {
      this.tags = tags;
      return this;
    }

    public MagpieGdriveResource.MagpieGdriveResourceBuilder withDiscoveryMeta(JsonNode discoveryMeta) {
      this.discoveryMeta = discoveryMeta;
      return this;
    }

    public MagpieGdriveResource build() {
      return new MagpieGdriveResource(this);
    }
  }
}

