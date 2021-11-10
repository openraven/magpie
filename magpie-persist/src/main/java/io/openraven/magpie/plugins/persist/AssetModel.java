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

package io.openraven.magpie.plugins.persist;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

// Keeping old structure for backward compatibility with rules
@Entity
@Table(name = "assets")
public class AssetModel {

  @Id
  @Column(name = "document_id", nullable = false, unique = true)
  public String documentId;

  @Column(name = "asset_id")
  @JsonAlias("arn")
  public String assetId;

  @Column(name = "resource_name")
  public String resourceName;

  @Column(name = "resource_id")
  public String resourceId;

  @Column(name = "resource_type")
  public String resourceType;

  @Column(name = "region")
  @JsonAlias("awsRegion")
  public String region;

  @Column(name = "project_id")
  public String projectId;

  @Column(name = "account_id")
  @JsonAlias("awsAccountId")
  public String accountId;

  @Column(name = "created_iso")
  public Instant createdIso;

  @Column(name = "updated_iso")
  public Instant updatedIso = Instant.now();

  @Column(name = "discovery_session_id")
  public String discoverySessionId;

  @Column(name = "max_size_in_bytes")
  public Long maxSizeInBytes = null;

  @Column(name = "size_in_bytes")
  public Long sizeInBytes = null;

  @Column(name = "configuration", columnDefinition = "JSONB")
  public String configuration;

  @Column(name = "supplementary_configuration", columnDefinition = "JSONB")
  public String supplementaryConfiguration;

  @Column(name = "tags", columnDefinition = "JSONB")
  public String tags;

  @Column(name = "discovery_meta", columnDefinition = "JSONB")
  public String discoveryMeta;

  public AssetModel() {
  }

  public String getDocumentId() {
    return documentId;
  }

  public void setDocumentId(String documentId) {
    this.documentId = documentId;
  }

  public String getAssetId() {
    return assetId;
  }

  public void setAssetId(String assetId) {
    this.assetId = assetId;
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

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
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

  public String getConfiguration() {
    return configuration;
  }

  @JsonSetter("configuration")
  public void setConfiguration(JsonNode configuration) {
    this.configuration = configuration.toPrettyString();
  }

  public String getSupplementaryConfiguration() {
    return supplementaryConfiguration;
  }

  @JsonSetter("supplementaryConfiguration")
  public void setSupplementaryConfiguration(JsonNode supplementaryConfiguration) {
    this.supplementaryConfiguration = supplementaryConfiguration.toString();
  }

  public String getTags() {
    return tags;
  }

  @JsonSetter("tags")
  public void setTags(JsonNode tags) {
    this.tags = tags.toString();
  }

  public String getDiscoveryMeta() {
    return discoveryMeta;
  }

  @JsonSetter("discoveryMeta")
  public void setDiscoveryMeta(JsonNode discoveryMeta) {
    this.discoveryMeta = discoveryMeta.toString();
  }
}
