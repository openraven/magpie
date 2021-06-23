package io.openraven.magpie.plugins.persist.mapper;

import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.plugins.persist.AssetModel;

import java.time.Instant;

public class AssetMapper {

  public AssetModel map(MagpieEnvelope env) {
    var contents = env.getContents();

    AssetModel assetModel = new AssetModel();
    assetModel.setDocumentId(contents.get("documentId").textValue());
    assetModel.setAssetId(contents.get("assetId").textValue());
    assetModel.setResourceName(contents.get("resourceName").textValue());
    assetModel.setResourceId(contents.get("resourceId").textValue());
    assetModel.setResourceType(contents.get("resourceType").textValue());
    assetModel.setRegion(contents.get("region").textValue());
    assetModel.setAccountId(contents.get("accountId").textValue());
    assetModel.setProjectId(contents.get("projectId").textValue());

    assetModel.setCreatedIso(
      contents.get("createdIso").isNull() ? null : Instant.parse(contents.get("createdIso").textValue()));
    assetModel.setUpdatedIso(
      contents.get("updatedIso").isNull() ? null : Instant.parse(contents.get("updatedIso").textValue()));

    assetModel.setDiscoverySessionId(contents.get("discoverySessionId").textValue());

    assetModel.setMaxSizeInBytes(contents.get("maxSizeInBytes").longValue());
    assetModel.setSizeInBytes(contents.get("sizeInBytes").longValue());

    assetModel.setConfiguration(contents.get("configuration").toPrettyString());
    assetModel.setSupplementaryConfiguration(contents.get("supplementaryConfiguration").toPrettyString());
    assetModel.setTags(contents.get("tags").toPrettyString());
    assetModel.setDiscoveryMeta(contents.get("discoveryMeta").toPrettyString());

    return assetModel;
  }
}
