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

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.time.Instant;

public interface AssetsDao {

  @SqlUpdate("DELETE FROM assets WHERE asset_id = :id")
  void removeRecord(@Bind("id") String assetId);

  @SqlUpdate("" +
    "INSERT INTO assets (" +
    "  document_id, " +
    "  asset_id, " +
    "  resource_name, " +
    "  resource_id, " +
    "  resource_type, " +
    "  region, " +
    "  account_id, " +
    "  project_id, " +
    "  created_iso, " +
    "  updated_iso, " +
    "  discovery_session_id, " +
    "  max_size_in_bytes, " +
    "  size_in_bytes, " +
    "  configuration, " +
    "  supplementary_configuration," +
    "  tags, " +
    "  discovery_meta) " +
    "VALUES(" +
    "  :documentId, " +
    "  :assetId, " +
    "  :resourceName, " +
    "  :resourceId, " +
    "  :resourceType, " +
    "  :region, " +
    "  :accountId, " +
    "  :projectId, " +
    "  CAST(:createdIso as TIMESTAMPTZ), " +
    "  CAST(:updatedIso as TIMESTAMPTZ), " +
    "  :discoverySessionId, " +
    "  :maxSizeInBytes, " +
    "  :sizeInBytes, " +
    "  CAST(:configuration as jsonb), " +
    "  CAST(:supplementaryConfiguration as jsonb)," +
    "  CAST(:tags as jsonb), " +
    "  CAST(:discoveryMeta as jsonb))")
  void insert(@BindBean AssetModel assetModel);
}
