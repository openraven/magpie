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
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.BackupVaultListMember;
import software.amazon.awssdk.services.backup.model.ListTagsRequest;
import software.amazon.awssdk.services.backup.model.ListTagsResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class BackupDiscovery implements AWSDiscovery {

  private static final String SERVICE = "backup";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return BackupClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    final var client = BackupClient.builder().region(region).build();

    final String RESOURCE_TYPE = "AWS::Backup::BackupVault";

    try {
      client.listBackupVaultsPaginator().stream()
        .forEach(backupVaultsResponse -> backupVaultsResponse.backupVaultList()
            .forEach(backupVault -> {
          var data = new AWSResource(backupVault.toBuilder(), region.toString(), account, mapper);
          data.resourceType = RESOURCE_TYPE;
          data.arn = backupVault.backupVaultArn();
          data.resourceName = backupVault.backupVaultName();
          data.resourceId = backupVault.backupVaultName();
          data.createdIso = backupVault.creationDate();

          discoverTags(client, backupVault, data);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":backupVault"), data.toJsonNode(mapper)));
        }));
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex, session.getId());
    }
  }

  private void discoverTags(BackupClient client, BackupVaultListMember resource, AWSResource data) {
    final String keyname = "tags";
    getAwsResponse(
      () -> client.listTagsPaginator(ListTagsRequest.builder().resourceArn(resource.backupVaultArn()).build())
        .stream()
        .map(ListTagsResponse::toBuilder)
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }
}
