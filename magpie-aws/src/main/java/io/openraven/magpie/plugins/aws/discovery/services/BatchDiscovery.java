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

package io.openraven.magpie.plugins.aws.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.api.Session;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.batch.BatchClient;

import java.util.List;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class BatchDiscovery implements AWSDiscovery {

  private static final String SERVICE = "batch";

  @FunctionalInterface
  interface LocalDiscovery {
    void discover(ObjectMapper mapper, Session session, BatchClient client, Region region, Emitter emitter, Logger logger);
  }

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return BatchClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger) {
    final var client = BatchClient.builder().region(region).build();

    final List<LocalDiscovery> methods = List.of(
      this::discoverComputeEnvironments,
      this::discoverJobQueues,
      this::discoverJobDefinitions
    );

    methods.forEach(m -> m.discover(mapper, session, client, region, emitter, logger));
  }

  private void discoverComputeEnvironments(ObjectMapper mapper, Session session, BatchClient client, Region region, Emitter emitter, Logger logger) {
    getAwsResponse(
      () -> client.describeComputeEnvironmentsPaginator().computeEnvironments(),
      (resp) -> resp.forEach(computeEnvironment -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", computeEnvironment.toBuilder());
        data.put("region", region.toString());

        emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":computeEnvironment"), data));
      }),
      (noresp) -> logger.error("Failed to get computeEnvironments in {}", region)
    );
  }

  private void discoverJobQueues(ObjectMapper mapper, Session session, BatchClient client, Region region, Emitter emitter, Logger logger) {
    getAwsResponse(
      () -> client.describeJobQueuesPaginator().jobQueues(),
      (resp) -> resp.forEach(computeEnvironment -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", computeEnvironment.toBuilder());
        data.put("region", region.toString());

        emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":jobQueue"), data));
      }),
      (noresp) -> logger.error("Failed to get jobQueues in {}", region)
    );

  }

  private void discoverJobDefinitions(ObjectMapper mapper, Session session, BatchClient client, Region region, Emitter emitter, Logger logger) {
    getAwsResponse(
      () -> client.describeJobDefinitionsPaginator().jobDefinitions(),
      (resp) -> resp.forEach(computeEnvironment -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", computeEnvironment.toBuilder());
        data.put("region", region.toString());

        emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":jobDefinition"), data));
      }),
      (noresp) -> logger.error("Failed to get jobDefinitions in {}", region)
    );
  }
}
