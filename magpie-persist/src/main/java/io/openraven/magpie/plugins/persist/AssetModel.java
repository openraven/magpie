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

import java.time.Instant;

public class AssetModel {

  public String documentId;
  public String assetId;
  public String resourceName;
  public String resourceId;
  public String resourceType;
  public String region;
  public String projectId;
  public String accountId;
  public Instant createdIso;
  public Instant updatedIso = Instant.now();
  public String discoverySessionId;

  public Long maxSizeInBytes = null;
  public Long sizeInBytes = null;

  public String configuration;
  public String supplementaryConfiguration;
  public String tags;
  public String discoveryMeta;

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

  public void setConfiguration(String configuration) {
    this.configuration = configuration;
  }

  public String getSupplementaryConfiguration() {
    return supplementaryConfiguration;
  }

  public void setSupplementaryConfiguration(String supplementaryConfiguration) {
    this.supplementaryConfiguration = supplementaryConfiguration;
  }

  public String getTags() {
    return tags;
  }

  public void setTags(String tags) {
    this.tags = tags;
  }

  public String getDiscoveryMeta() {
    return discoveryMeta;
  }

  public void setDiscoveryMeta(String discoveryMeta) {
    this.discoveryMeta = discoveryMeta;
  }
}
