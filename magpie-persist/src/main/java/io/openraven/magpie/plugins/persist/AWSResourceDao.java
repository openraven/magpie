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
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface AWSResourceDao {
  @SqlQuery("SELECT count(*) FROM information_schema.tables WHERE table_name = :table")
  String doesTableExist(@Bind("table") String name);

  @SqlUpdate("" +
    "CREATE TABLE <table> (" +
    "  documentid TEXT PRIMARY KEY, " +
    "  arn TEXT, " +
    "  resourcename TEXT, " +
    "  resourceid TEXT, " +
    "  resourcetype TEXT, " +
    "  awsregion TEXT, " +
    "  creatediso TIMESTAMPTZ, " +
    "  updatediso TIMESTAMPTZ, " +
    "  discoverysessionid TEXT, " +
    "  maxsizeinbytes INTEGER, " +
    "  sizeinbytes INTEGER, " +
    "  configuration JSONB, " +
    "  supplementaryconfiguration JSONB, " +
    "  tags JSONB, " +
    "  discoverymeta JSONB)")
  void createTable(@Define("table") String name);

  @SqlUpdate("DELETE FROM <table> WHERE documentId=:id")
  void removeRecord(@Define("table") String name, @Bind("id") String documentId);

  @SqlUpdate("" +
    "INSERT INTO <table> (" +
    "  documentid, " +
    "  arn, " +
    "  resourcename, " +
    "  resourceid, " +
    "  resourcetype, " +
    "  awsregion, " +
    "  creatediso, " +
    "  updatediso, " +
    "  discoverysessionid, " +
    "  maxsizeinbytes, " +
    "  sizeinbytes, " +
    "  configuration, " +
    "  supplementaryconfiguration," +
    "  tags, " +
    "  discoverymeta) " +
    "VALUES(" +
    "  :documentId, " +
    "  :arn, " +
    "  :resourceName, " +
    "  :resourceId, " +
    "  :resourceType, " +
    "  :awsRegion, " +
    "  CAST(:createdIso as TIMESTAMPTZ), " +
    "  CAST(:updatedIso as TIMESTAMPTZ), " +
    "  :discoverySessionId, " +
    "  :maxSizeInBytes, " +
    "  :sizeInBytes, " +
    "  CAST(:configuration as jsonb), " +
    "  CAST(:supplementaryConfiguration as jsonb)," +
    "  CAST(:tags as jsonb), " +
    "  CAST(:discoveryMeta as jsonb))")
  void insert(@Define("table") String table,
              @Bind("documentId") String documentId,
              @Bind("arn") String arn,
              @Bind("resourceName") String resourceName,
              @Bind("resourceId") String resourceId,
              @Bind("resourceType") String resourceType,
              @Bind("awsRegion") String awsRegion,
              @Bind("createdIso") String createdIso,
              @Bind("updatedIso") String updatedIso,
              @Bind("discoverySessionId") String discoverySessionId,
              @Bind("maxSizeInBytes") long maxSizeInBytes,
              @Bind("sizeInBytes") long sizeInBytes,
              @Bind("configuration") String configuration,
              @Bind("supplementaryConfiguration") String supplementaryConfiguration,
              @Bind("tags") String tags,
              @Bind("discoveryMeta") String discoveryMeta
  );
}
