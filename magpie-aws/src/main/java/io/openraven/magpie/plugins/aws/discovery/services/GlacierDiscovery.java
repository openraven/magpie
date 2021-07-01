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
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.glacier.GlacierClient;
import software.amazon.awssdk.services.glacier.model.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class GlacierDiscovery implements AWSDiscovery {

  private static final String SERVICE = "glacier";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return GlacierClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    final var client = AWSUtils.configure(GlacierClient.builder(), region);
    final String RESOURCE_TYPE = "AWS::Glacier::Vault";

    try {
      client.listVaultsPaginator().vaultList().stream().forEach(vault -> {
        var data = new MagpieResource.MagpieResourceBuilder(mapper, vault.vaultARN())
          .withResourceName(vault.vaultName())
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(vault.toBuilder()))
          .withCreatedIso(Instant.parse(vault.creationDate()))
          .withSizeInBytes(vault.sizeInBytes())
          .withAccountId(account)
          .withRegion(region.toString())
          .build();

        discoverJobs(client, vault, data);
        discoverMultipartUploads(client, vault, data);
        discoverAccessPolicy(client, vault, data);
        discoverVaultLock(client, vault, data);
        discoverVaultNotifications(client, vault, data);
        discoverTags(client, vault, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":vault"), data.toJsonNode()));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverJobs(GlacierClient client, DescribeVaultOutput resource, MagpieResource data) {
    final String keyname = "jobs";

    getAwsResponse(
      () -> client.listJobsPaginator(ListJobsRequest.builder().vaultName(resource.vaultName()).build()).jobList()
        .stream()
        .map(GlacierJobDescription::toBuilder)
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverMultipartUploads(GlacierClient client, DescribeVaultOutput resource, MagpieResource data) {
    final String keyname = "multipartUploads";

    getAwsResponse(
      () -> client.listMultipartUploadsPaginator(ListMultipartUploadsRequest.builder().vaultName(resource.vaultName()).build()).uploadsList()
        .stream()
        .map(UploadListElement::toBuilder)
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverAccessPolicy(GlacierClient client, DescribeVaultOutput resource, MagpieResource data) {
    final String keyname = "accessPolicy";

    getAwsResponse(
      () -> client.getVaultAccessPolicy(GetVaultAccessPolicyRequest.builder().vaultName(resource.vaultName()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverVaultNotifications(GlacierClient client, DescribeVaultOutput resource, MagpieResource data) {
    final String keyname = "vaultNotifications";

    getAwsResponse(
      () -> client.getVaultNotifications(GetVaultNotificationsRequest.builder().vaultName(resource.vaultName()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverVaultLock(GlacierClient client, DescribeVaultOutput resource, MagpieResource data) {
    final String keyname = "vaultLock";

    getAwsResponse(
      () -> client.getVaultLock(GetVaultLockRequest.builder().vaultName(resource.vaultName()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverTags(GlacierClient client, DescribeVaultOutput resource, MagpieResource data) {
    final String keyname = "tags";

    getAwsResponse(
      () -> client.listTagsForVault(ListTagsForVaultRequest.builder().vaultName(resource.vaultName()).build()).tags(),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }
}
