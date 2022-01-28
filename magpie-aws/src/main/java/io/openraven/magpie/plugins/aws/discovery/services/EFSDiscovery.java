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
import io.openraven.magpie.data.aws.efs.EfsFileSystem;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.efs.EfsClient;
import software.amazon.awssdk.services.efs.model.DescribeMountTargetsRequest;
import software.amazon.awssdk.services.efs.model.FileSystemDescription;

import java.util.List;
import java.util.Map;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class EFSDiscovery implements AWSDiscovery {

  private static final String SERVICE = "efs";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return EfsClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {
    final String RESOURCE_TYPE = EfsFileSystem.RESOURCE_TYPE;

    try (final var client = clientCreator.apply(EfsClient.builder()).build()) {
      client.describeFileSystems().fileSystems().forEach(fileSystem -> {
        String arn = String.format("arn:aws:elasticfilesystem:%s:%s:file-system/%s", region, fileSystem.ownerId(), fileSystem.fileSystemId());
        var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, arn)
          .withResourceName(fileSystem.name())
          .withResourceId(fileSystem.fileSystemId())
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(fileSystem.toBuilder()))
          .withCreatedIso(fileSystem.creationTime())
          .withSizeInBytes(fileSystem.sizeInBytes().value())
          .withAccountId(fileSystem.ownerId())
          .withAwsRegion(region.toString())
          .build();

        discoverMountTargets(client, fileSystem, data);
        discoverBackupJobs(arn, region, data, clientCreator, logger);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":fileSystem"), data.toJsonNode()));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverMountTargets(EfsClient client, FileSystemDescription resource, MagpieAwsResource data) {
    final String keyname = "mountTargets";

    getAwsResponse(
      () -> client.describeMountTargets(DescribeMountTargetsRequest.builder().fileSystemId(resource.fileSystemId()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }
}
