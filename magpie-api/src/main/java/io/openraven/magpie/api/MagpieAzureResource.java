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

public class MagpieAzureResource {
  private final ObjectMapper mapper;

  public String arn;
  public String resourceName;
  public String resourceId;
  public String resourceType;
  public String awsRegion;
  public String awsAccountId;
  public Instant createdIso;
  public Instant updatedIso = Instant.now();
  public String discoverySessionId;
  public Long maxSizeInBytes = null;
  public Long sizeInBytes = null;

  public JsonNode configuration;
  public JsonNode supplementaryConfiguration;
  public JsonNode tags;
  public JsonNode discoveryMeta;

  private MagpieAzureResource(MagpieAwsResourceBuilder builder) {
    this.arn = builder.arn;
    this.resourceName = builder.resourceName;
    this.resourceId = builder.resourceId;
    this.resourceType = builder.resourceType;
    this.awsRegion = builder.awsRegion;
    this.awsAccountId = builder.awsAccountId;
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

    data.put("documentId", getEncodedNamedUUID(arn));
    data.put("arn", arn);
    data.put("resourceName", resourceName);
    data.put("resourceId", resourceId);
    data.put("resourceType", resourceType);
    data.put("awsRegion", awsRegion);
    data.put("awsAccountId", awsAccountId);
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

  public String getArn() {
    return arn;
  }

  public void setArn(String arn) {
    this.arn = arn;
  }

  public String getResourceName() {
    return resourceName;
  }

  public void setResourceName(String resourceName) {
    this.resourceName = resourceName;
  }

  public String getResourceId() {
    return resourceId;
  }

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  public String getResourceType() {
    return resourceType;
  }

  public void setResourceType(String resourceType) {
    this.resourceType = resourceType;
  }

  public String getAwsRegion() {
    return awsRegion;
  }

  public void setAwsRegion(String awsRegion) {
    this.awsRegion = awsRegion;
  }

  public String getAwsAccountId() {
    return awsAccountId;
  }

  public void setAwsAccountId(String awsAccountId) {
    this.awsAccountId = awsAccountId;
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

  public static class MagpieAwsResourceBuilder {
    private final ObjectMapper mapper;
    private String arn;
    private String resourceName;
    private String resourceId;
    private String resourceType;
    private String awsRegion;
    private String awsAccountId;
    private Instant createdIso;
    private Instant updatedIso = Instant.now();
    private String discoverySessionId;

    private Long maxSizeInBytes = null;
    private Long sizeInBytes = null;

    private JsonNode configuration;
    private JsonNode supplementaryConfiguration;
    private JsonNode tags;
    private JsonNode discoveryMeta;

    public MagpieAwsResourceBuilder(ObjectMapper mapper, String arn) {
      this.arn = arn;
      this.mapper = mapper;

      this.configuration = mapper.createObjectNode();
      this.supplementaryConfiguration = mapper.createObjectNode();
      this.tags = mapper.createObjectNode();
      this.discoveryMeta = mapper.createObjectNode();
    }

    public MagpieAwsResourceBuilder withResourceName(String resourceName) {
      this.resourceName = resourceName;
      return this;
    }

    public MagpieAwsResourceBuilder withResourceId(String resourceId) {
      this.resourceId = resourceId;
      return this;
    }

    public MagpieAwsResourceBuilder withResourceType(String resourceType) {
      this.resourceType = resourceType;
      return this;
    }

    public MagpieAwsResourceBuilder withAwsRegion(String region) {
      this.awsRegion = region;
      return this;
    }

    public MagpieAwsResourceBuilder withAccountId(String accountId) {
      this.awsAccountId = accountId;
      return this;
    }

    public MagpieAwsResourceBuilder withCreatedIso(Instant createdIso) {
      this.createdIso = createdIso;
      return this;
    }

    public MagpieAwsResourceBuilder withUpdatedIso(Instant updatedIso) {
      this.updatedIso = updatedIso;
      return this;
    }

    public MagpieAwsResourceBuilder withDiscoverySessionId(String discoverySessionId) {
      this.discoverySessionId = discoverySessionId;
      return this;
    }

    public MagpieAwsResourceBuilder withMaxSizeInBytes(Long maxSizeInBytes) {
      this.maxSizeInBytes = maxSizeInBytes;
      return this;
    }

    public MagpieAwsResourceBuilder withSizeInBytes(Long sizeInBytes) {
      this.sizeInBytes = sizeInBytes;
      return this;
    }

    public MagpieAwsResourceBuilder withConfiguration(JsonNode configuration) {
      this.configuration = configuration;
      return this;
    }

    public MagpieAwsResourceBuilder withSupplementaryConfiguration(JsonNode supplementaryConfiguration) {
      this.supplementaryConfiguration = supplementaryConfiguration;
      return this;
    }

    public MagpieAwsResourceBuilder withTags(JsonNode tags) {
      this.tags = tags;
      return this;
    }

    public MagpieAwsResourceBuilder withDiscoveryMeta(JsonNode discoveryMeta) {
      this.discoveryMeta = discoveryMeta;
      return this;
    }

    public MagpieAzureResource build() {
      return new MagpieAzureResource(this);
    }
  }
}
