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
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
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
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    final var client = EfsClient.builder().region(region).build();

    getAwsResponse(
      () -> client.describeFileSystems().fileSystems(),
      (resp) -> resp.forEach(fileSystem -> {
        var data = new AWSResource(fileSystem.toBuilder(), region.toString(), account, mapper);
        data.resourceId = fileSystem.fileSystemId();
        data.awsAccountId = fileSystem.ownerId();
        data.arn = String.format("arn:aws:elasticfilesystem:%s:%s:file-system/%s", region.toString(), fileSystem.ownerId(), fileSystem.fileSystemId());
        data.resourceName = fileSystem.name();
        data.resourceType = "AWS::EFS::FileSystem";
        data.createdIso = fileSystem.creationTime().toString();
        data.sizeInBytes = fileSystem.sizeInBytes().value();

        discoverMountTargets(client, fileSystem, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":fileSystem"), data.toJsonNode(mapper)));
      }),
      (noresp) -> logger.error("Failed to get fileSystems in {}", region)
    );
  }

  private void discoverMountTargets(EfsClient client, FileSystemDescription resource, AWSResource data) {
    final String keyname = "mountTargets";

    getAwsResponse(
      () -> client.describeMountTargets(DescribeMountTargetsRequest.builder().fileSystemId(resource.fileSystemId()).build()),
      (resp) -> AWSUtils.update(data.configuration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.configuration, Map.of(keyname, noresp))
    );
  }
}
