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
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSResource;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.batch.BatchClient;

import java.util.List;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class BatchDiscovery implements AWSDiscovery {

  private static final String SERVICE = "batch";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return BatchClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    final var client = BatchClient.builder().region(region).build();

    discoverComputeEnvironments(mapper, session, client, region, emitter, logger, account);
    discoverJobQueues(mapper, session, client, region, emitter, logger, account);
    discoverJobDefinitions(mapper, session, client, region, emitter, logger, account);
  }

  private void discoverComputeEnvironments(ObjectMapper mapper, Session session, BatchClient client, Region region, Emitter emitter, Logger logger, String account) {
    getAwsResponse(
      () -> client.describeComputeEnvironmentsPaginator().computeEnvironments(),
      (resp) -> resp.forEach(computeEnvironment -> {
        var data = new AWSResource(computeEnvironment.toBuilder(), region.toString(), account, mapper);
        data.arn = computeEnvironment.computeEnvironmentArn();
        data.resourceName = computeEnvironment.computeEnvironmentName();
        data.resourceType = "AWS::Batch::ComputeEnvironment";

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":computeEnvironment"), data.toJsonNode(mapper)));
      }),
      (noresp) -> logger.error("Failed to get computeEnvironments in {}", region)
    );
  }

  private void discoverJobQueues(ObjectMapper mapper, Session session, BatchClient client, Region region, Emitter emitter, Logger logger, String account) {
    getAwsResponse(
      () -> client.describeJobQueuesPaginator().jobQueues(),
      (resp) -> resp.forEach(jobQueue -> {
        var data = new AWSResource(jobQueue.toBuilder(), region.toString(), account, mapper);
        data.arn = jobQueue.jobQueueArn();
        data.resourceName = jobQueue.jobQueueName();
        data.resourceType = "AWS::Batch::JobQueue";

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":jobQueue"), data.toJsonNode(mapper)));
      }),
      (noresp) -> logger.error("Failed to get jobQueues in {}", region)
    );

  }

  private void discoverJobDefinitions(ObjectMapper mapper, Session session, BatchClient client, Region region, Emitter emitter, Logger logger, String account) {
    getAwsResponse(
      () -> client.describeJobDefinitionsPaginator().jobDefinitions(),
      (resp) -> resp.forEach(jobDefinition -> {
        var data = new AWSResource(jobDefinition.toBuilder(), region.toString(), account, mapper);
        data.arn = jobDefinition.jobDefinitionArn();
        data.resourceName = jobDefinition.jobDefinitionName();
        data.resourceType = "AWS::Batch::JobDefinition";

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":jobDefinition"), data.toJsonNode(mapper)));
      }),
      (noresp) -> logger.error("Failed to get jobDefinitions in {}", region)
    );
  }
}
