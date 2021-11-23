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
import io.openraven.magpie.api.MagpieAwsResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.aws.fsx.FSxFileSystem;
import io.openraven.magpie.plugins.aws.discovery.*;
import org.javatuples.Pair;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.fsx.FSxClient;
import software.amazon.awssdk.services.fsx.model.FileSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getCloudwatchMetricMinimum;

public class FSXDiscovery implements AWSDiscovery {

  private static final String SERVICE = "fsx";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return FSxClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {
    final String RESOURCE_TYPE = FSxFileSystem.RESOURCE_TYPE;

    try (final var client = clientCreator.apply(FSxClient.builder()).build()) {
      client.describeFileSystems().fileSystems().forEach(fileSystem -> {
        var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, fileSystem.resourceARN())
          .withResourceName(fileSystem.fileSystemId())
          .withResourceId(fileSystem.fileSystemId())
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(fileSystem.toBuilder()))
          .withCreatedIso(fileSystem.creationTime())
          .withAccountId(account)
          .withAwsRegion(region.toString())
          .build();

        discoverSize(fileSystem, data, region, logger, clientCreator);
        discoverBackupJobs(fileSystem.resourceARN(), region, data, clientCreator);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":fileSystem"), data.toJsonNode()));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverSize(FileSystem resource, MagpieAwsResource data, Region region, Logger logger, MagpieAWSClientCreator clientCreator) {
    try {
      List<Dimension> dimensions = new ArrayList<>();
      dimensions.add(Dimension.builder().name("FileSystemId").value(resource.fileSystemId()).build());
      Pair<Long, GetMetricStatisticsResponse> freeStorageCapacity =
        getCloudwatchMetricMinimum(region.toString(), "AWS/FSx", "FreeDataStorageCapacity", dimensions, clientCreator);

      long capacityAsBytes = Conversions.GibToBytes(resource.storageCapacity());

      data.sizeInBytes = capacityAsBytes - freeStorageCapacity.getValue0();
      data.maxSizeInBytes = capacityAsBytes;

      AWSUtils.update(data.supplementaryConfiguration,
        Map.of("freeDataStorageCapacity", freeStorageCapacity.getValue0()));
    } catch (Exception ex) {
      logger.debug("Failure on FSX size discovery, Region - {}; ResourceArn - {}",
        region, resource.resourceARN(), ex);
    }
  }
}
