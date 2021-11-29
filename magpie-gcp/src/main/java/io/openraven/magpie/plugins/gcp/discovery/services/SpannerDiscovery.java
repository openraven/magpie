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

package io.openraven.magpie.plugins.gcp.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.repackaged.com.google.common.base.Pair;
import com.google.cloud.spanner.admin.database.v1.DatabaseAdminClient;
import com.google.cloud.spanner.admin.instance.v1.InstanceAdminClient;
import com.google.spanner.admin.database.v1.Backup;
import com.google.spanner.admin.database.v1.Database;
import com.google.spanner.admin.instance.v1.Instance;
import com.google.spanner.admin.instance.v1.ProjectName;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gcp.spanner.SpannerInstance;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SpannerDiscovery implements GCPDiscovery {
  private static final String SERVICE = "spanner";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger) {
    final String RESOURCE_TYPE = SpannerInstance.RESOURCE_TYPE;

    try (var client = InstanceAdminClient.create()) {
      ProjectName projectName = ProjectName.of(projectId);

      client.listInstances(projectName).iterateAll().forEach(instance -> {
        var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, instance.getName())
          .withProjectId(projectId)
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(GCPUtils.asJsonNode(instance))
          .build();

        try (var databaseAdminClient = DatabaseAdminClient.create()) {
          discoverBackups(instance, data, databaseAdminClient);
          discoverDatabases(instance, data, databaseAdminClient);
        } catch (IOException e) {
          DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
        }

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":instance"), data.toJsonNode()));
      });
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverBackups(Instance instance, MagpieGcpResource data, DatabaseAdminClient databaseAdminClient) {
    final String fieldName = "backups";

    ArrayList<Backup.Builder> list = new ArrayList<>();

    databaseAdminClient.listBackups(instance.getName()).iterateAll()
      .forEach(task -> list.add(task.toBuilder()));

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, list));
  }

  private void discoverDatabases(Instance instance, MagpieGcpResource data, DatabaseAdminClient databaseAdminClient) {
    final String fieldName = "databases";

    ArrayList<Database.Builder> list = new ArrayList<>();

    databaseAdminClient.listDatabases(instance.getName()).iterateAll()
      .forEach(task -> list.add(task.toBuilder()));

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, list));
  }
}
