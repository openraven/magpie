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
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.batch.BatchClient;

import java.util.List;

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
    final var client = AWSUtils.configure(BatchClient.builder(), region);

    discoverComputeEnvironments(mapper, session, client, region, emitter, account);
    discoverJobQueues(mapper, session, client, region, emitter, account);
    discoverJobDefinitions(mapper, session, client, region, emitter, account);
  }

  private void discoverComputeEnvironments(ObjectMapper mapper, Session session, BatchClient client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::Batch::ComputeEnvironment";

    try {
      client.describeComputeEnvironmentsPaginator().computeEnvironments()
        .forEach( computeEnvironment -> {
          var data = new AWSResource(computeEnvironment.toBuilder(), region.toString(), account, mapper);
          data.arn = computeEnvironment.computeEnvironmentArn();
          data.resourceName = computeEnvironment.computeEnvironmentName();
          data.resourceType = RESOURCE_TYPE;

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":computeEnvironment"), data.toJsonNode(mapper)));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverJobQueues(ObjectMapper mapper, Session session, BatchClient client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::Batch::JobQueue";

    try {
      client.describeJobQueuesPaginator().jobQueues().forEach(jobQueue -> {
        var data = new AWSResource(jobQueue.toBuilder(), region.toString(), account, mapper);
        data.arn = jobQueue.jobQueueArn();
        data.resourceName = jobQueue.jobQueueName();
        data.resourceType = RESOURCE_TYPE;

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":jobQueue"), data.toJsonNode(mapper)));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverJobDefinitions(ObjectMapper mapper, Session session, BatchClient client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::Batch::JobDefinition";

    try {
      client.describeJobDefinitionsPaginator().jobDefinitions().forEach(jobDefinition -> {
        var data = new AWSResource(jobDefinition.toBuilder(), region.toString(), account, mapper);
        data.arn = jobDefinition.jobDefinitionArn();
        data.resourceName = jobDefinition.jobDefinitionName();
        data.resourceType = RESOURCE_TYPE;

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":jobDefinition"), data.toJsonNode(mapper)));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }
}
