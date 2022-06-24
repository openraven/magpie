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
package io.openraven.magpie.data.gcp;

import com.fasterxml.jackson.databind.JsonNode;
import io.openraven.magpie.data.Resource;
import io.openraven.magpie.data.utils.JsonConverter;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.time.Instant;

@Access(AccessType.FIELD)
@MappedSuperclass
public class GCPResource extends Resource {
    @Id
    @Column(name = "documentid", columnDefinition = "TEXT",
            nullable = false, unique = true)
    public String documentId;
    @Column(name = "assetid", columnDefinition = "TEXT")
    public String assetId;
    @Column(name = "resourcename", columnDefinition = "TEXT")
    public String resourceName;
    @Column(name = "resourceid", columnDefinition = "TEXT")
    public String resourceId;
    @Column(name = "resourcetype", columnDefinition = "TEXT", updatable = false)
    public String resourceType;
    @Column(name = "region", columnDefinition = "TEXT")
    public String region;
    @Column(name = "projectid", columnDefinition = "TEXT")
    public String projectId;
    @Column(name = "gcpaccountid", columnDefinition = "TEXT")
    public String gcpAccountId;
    @Column(name = "creatediso", columnDefinition = "TIMESTAMPTZ")
    public Instant createdIso;
    @Column(name = "updatediso", columnDefinition = "TIMESTAMPTZ")
    public Instant updatedIso = Instant.now();
    @Column(name = "discoverysessionid", columnDefinition = "TEXT")
    public String discoverySessionId;

    @Transient
    public Long maxSizeInBytes = null;
    @Transient
    public Long sizeInBytes = null;

    @Column(name = "configuration", columnDefinition = "JSONB")
    @Convert(converter = JsonConverter.class)
    public JsonNode configuration;

    @Column(name = "supplementaryconfiguration", columnDefinition = "JSONB")
    @Convert(converter = JsonConverter.class)
    public JsonNode supplementaryConfiguration;

    @Column(name = "tags", columnDefinition = "JSONB")
    @Convert(converter = JsonConverter.class)
    public JsonNode tags;

    @Column(name = "discoverymeta", columnDefinition = "JSONB")
    @Convert(converter = JsonConverter.class)
    public JsonNode discoveryMeta;

    public GCPResource() {
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

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getGcpAccountId() {
        return gcpAccountId;
    }

    public void setGcpAccountId(String gcpAccountId) {
        this.gcpAccountId = gcpAccountId;
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

    public String getRegion() {
      return region;
    }

    public void setRegion(String region) {
      this.region = region;
    }


}
