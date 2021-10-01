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
