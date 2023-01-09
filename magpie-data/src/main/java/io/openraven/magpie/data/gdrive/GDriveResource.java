package io.openraven.magpie.data.gdrive;


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
public class GDriveResource extends Resource {
  @Id
  @Column(name = "documentid", columnDefinition = "TEXT",
    nullable = false, unique = true)
  public String documentId;

  @Column(name = "assetid", columnDefinition = "TEXT")
  public String assetId;
  @Column(name = "resourcename", columnDefinition = "TEXT")
  public String resourceName;

  // this needs to be updatable = false due to its use a a discriminator
  @Column(name = "resourcetype", columnDefinition = "TEXT", updatable = false)
  public String resourceType;
  @Column(name = "drive", columnDefinition = "TEXT", updatable = false)
  public String drive;

  @Column(name = "domain", columnDefinition = "TEXT")
  public String domain;
  @Column(name = "creatediso", columnDefinition = "TIMESTAMPTZ")
  public Instant createdIso;
  @Column(name = "updatediso", columnDefinition = "TIMESTAMPTZ")
  public Instant updatedIso;
  @Column(name = "discoverysessionid", columnDefinition = "TEXT")
  public String discoverySessionId;

  @Transient
  public Long maxSizeInBytes;
  @Transient
  public Long sizeInBytes;

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

  public GDriveResource() {
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
}
