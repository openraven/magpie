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
import com.google.api.gax.core.CredentialsProvider;
import com.google.appengine.repackaged.com.google.common.base.Pair;
import com.google.cloud.iot.v1.LocationName;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.CloudTasksSettings;
import com.google.cloud.tasks.v2.Queue;
import com.google.cloud.tasks.v2.Task;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gcp.task.TaskQueue;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TasksDiscovery implements GCPDiscovery {
  private static final String SERVICE = "tasks";

  private static final List<String> AVAILABLE_LOCATIONS = List.of(
    "asia-east1",
    "asia-east2",
    "asia-northeast1",
    "asia-northeast2",
    "asia-northeast3",
    "asia-south1",
    "asia-south2",
    "asia-southeast1",
    "asia-southeast2",
    "australia-southeast1",
    "australia-southeast2",
    "europe-central2",
    "europe-north1",
    "europe-west1",
    "europe-west2",
    "europe-west3",
    "europe-west4",
    "europe-west5",
    "europe-west6",
    "northamerica-northeast1",
    "northamerica-northeast2",
    "southamerica-east1",
    "us-central1",
    "us-central2",
    "us-east1",
    "us-east4",
    "us-west1",
    "us-west2",
    "us-west3",
    "us-west4");

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger, Optional<CredentialsProvider> maybeCredentialsProvider) {
    final String RESOURCE_TYPE = TaskQueue.RESOURCE_TYPE;
    var builder = CloudTasksSettings.newBuilder();
    maybeCredentialsProvider.ifPresent(builder::setCredentialsProvider);

    try (CloudTasksClient cloudTasksClient = CloudTasksClient.create(builder.build())) {
      AVAILABLE_LOCATIONS.forEach(location -> {
        try {
          LocationName parent = LocationName.of(projectId, location);
          for (Queue element : cloudTasksClient.listQueues(parent.toString()).iterateAll()) {
            var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, element.getName())
              .withProjectId(projectId)
              .withResourceType(RESOURCE_TYPE)
              .withRegion(location)
              .withConfiguration(GCPUtils.asJsonNode(element))
              .build();

//            discoverTasks(cloudTasksClient, element, data);  // See PROD-10053 (OOM)

            emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":queue"), data.toJsonNode()));
          }
        } catch (Exception ex) {
          DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, ex);
        }
      });
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverTasks(CloudTasksClient client,
                             Queue queue,
                             MagpieGcpResource data) {
    final String fieldName = "tasks";

    ArrayList<Task.Builder> list = new ArrayList<>();
    client.listTasks(queue.getName()).iterateAll()
      .forEach(task -> list.add(task.toBuilder()));

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, list));
  }
}
