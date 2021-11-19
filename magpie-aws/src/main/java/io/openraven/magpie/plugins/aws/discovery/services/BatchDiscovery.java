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
import io.openraven.magpie.api.MagpieResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
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
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {
    try (final var client = clientCreator.apply(BatchClient.builder()).build()) {
      discoverComputeEnvironments(mapper, session, client, region, emitter, account);
      discoverJobQueues(mapper, session, client, region, emitter, account);
      discoverJobDefinitions(mapper, session, client, region, emitter, account);
    }
  }

  private void discoverComputeEnvironments(ObjectMapper mapper, Session session, BatchClient client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::Batch::ComputeEnvironment";

    try {
      client.describeComputeEnvironmentsPaginator().computeEnvironments()
        .forEach( computeEnvironment -> {
          var data = new MagpieResource.MagpieResourceBuilder(mapper, computeEnvironment.computeEnvironmentArn())
            .withResourceName(computeEnvironment.computeEnvironmentName())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(computeEnvironment.toBuilder()))
            .withAccountId(account)
            .withRegion(region.toString())
            .build();

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":computeEnvironment"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverJobQueues(ObjectMapper mapper, Session session, BatchClient client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::Batch::JobQueue";

    try {
      client.describeJobQueuesPaginator().jobQueues().forEach(jobQueue -> {
        var data = new MagpieResource.MagpieResourceBuilder(mapper, jobQueue.jobQueueArn())
          .withResourceName(jobQueue.jobQueueName())
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(jobQueue.toBuilder()))
          .withAccountId(account)
          .withRegion(region.toString())
          .build();

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":jobQueue"), data.toJsonNode()));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverJobDefinitions(ObjectMapper mapper, Session session, BatchClient client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::Batch::JobDefinition";

    try {
      client.describeJobDefinitionsPaginator().jobDefinitions().forEach(jobDefinition -> {
        var data = new MagpieResource.MagpieResourceBuilder(mapper, jobDefinition.jobDefinitionArn())
          .withResourceName(jobDefinition.jobDefinitionName())
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(jobDefinition.toBuilder()))
          .withAccountId(account)
          .withRegion(region.toString())
          .build();

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":jobDefinition"), data.toJsonNode()));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }
}
