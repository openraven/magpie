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

import com.google.appengine.repackaged.com.google.common.base.Pair;
import com.google.cloud.bigtable.admin.v2.BigtableInstanceAdminClient;
import com.google.cloud.bigtable.admin.v2.models.Cluster;
import com.google.cloud.bigtable.admin.v2.models.Instance;
import com.google.cloud.bigtable.admin.v2.models.PartialListInstancesException;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPResource;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BigTableDiscovery implements GCPDiscovery {
  private static final String SERVICE = "bigTable";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(String projectId, Session session, Emitter emitter, Logger logger) {
    final String RESOURCE_TYPE = "GCP::BigTable::Instance";

    try (BigtableInstanceAdminClient client = BigtableInstanceAdminClient.create(projectId)) {
      List<Instance> instances = new ArrayList<>();
      try {
        instances = client.listInstances();
      } catch (PartialListInstancesException e) {
        logger.error("The following zones are unavailable: " + e.getUnavailableZones());
        logger.error("But the following instances are reachable: " + e.getInstances());
      }

      instances.listIterator().forEachRemaining(instance -> {
        var data = new GCPResource(instance.getId(), projectId, RESOURCE_TYPE);
        data.configuration = GCPUtils.asJsonNode(instance);

        discoverClusters(client, instance, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":instance"), data.toJsonNode()));
      });
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverClusters(BigtableInstanceAdminClient client, Instance instance, GCPResource data) {
    final String fieldName = "clusters";

    ArrayList<Cluster> list = new ArrayList<>();

    client.listClusters(instance.getId()).listIterator()
      .forEachRemaining(list::add);

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, list));
  }
}
