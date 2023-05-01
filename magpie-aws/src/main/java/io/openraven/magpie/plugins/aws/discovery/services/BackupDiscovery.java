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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieAwsResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.aws.backup.BackupPlan;
import io.openraven.magpie.data.aws.backup.BackupVault;
import io.openraven.magpie.plugins.aws.discovery.AWSDiscoveryConfig;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.BackupPlansListMember;
import software.amazon.awssdk.services.backup.model.BackupVaultListMember;
import software.amazon.awssdk.services.backup.model.GetBackupPlanRequest;
import software.amazon.awssdk.services.backup.model.GetBackupSelectionRequest;
import software.amazon.awssdk.services.backup.model.GetBackupSelectionResponse;
import software.amazon.awssdk.services.backup.model.ListBackupSelectionsRequest;
import software.amazon.awssdk.services.backup.model.ListTagsRequest;

import java.util.ArrayList;
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
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator, AWSDiscoveryConfig config) {
    try (final var client = clientCreator.apply(BackupClient.builder()).build()) {
      discoverVaults(mapper, session, region, emitter, account, client);
      discoverPlans(mapper, session, region, emitter, logger, account, client);
    }
  }

  public void discoverPlans(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, BackupClient client) {
    final var RESOURCE_TYPE = BackupPlan.RESOURCE_TYPE;

    client.listBackupPlans().backupPlansList().forEach(backupPlan -> {

      var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, backupPlan.backupPlanArn())
        .withResourceName(backupPlan.backupPlanName())
        .withResourceId(backupPlan.backupPlanId())
        .withResourceType(RESOURCE_TYPE)
        .withConfiguration(mapper.valueToTree(backupPlan.toBuilder()))
        .withCreatedIso(backupPlan.creationDate())
        .withAccountId(account)
        .withAwsRegion(region.toString())
        .build();

      getPlanDetails(client, data);
      discoverTags(client, backupPlan, data, mapper);

      var backupSelections = new ArrayList<GetBackupSelectionResponse.Builder>();
      client.listBackupSelectionsPaginator(ListBackupSelectionsRequest.builder().backupPlanId(backupPlan.backupPlanId()).build()).forEach(listBackupSelectionsResp -> listBackupSelectionsResp.backupSelectionsList().forEach(backupSelection -> {
        var backupSelectionResp = client.getBackupSelection(GetBackupSelectionRequest.builder().backupPlanId(backupSelection.backupPlanId()).selectionId(backupSelection.selectionId()).build());
        backupSelections.add(backupSelectionResp.toBuilder());
      }));
      AWSUtils.update(data.supplementaryConfiguration, Map.of("backupSelections", backupSelections));
      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":backupVault"), data.toJsonNode()));
    });

  }

  public void discoverVaults(ObjectMapper mapper, Session session, Region region, Emitter emitter, String account, BackupClient client) {
    final var RESOURCE_TYPE = BackupVault.RESOURCE_TYPE;
    try {
      client.listBackupVaults().backupVaultList().stream()
          .forEach(backupVault -> {
            var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, backupVault.backupVaultArn())
              .withResourceName(backupVault.backupVaultName())
              .withResourceId(backupVault.backupVaultName())
              .withResourceType(RESOURCE_TYPE)
              .withCreatedIso(backupVault.creationDate())
              .withAccountId(account)
              .withAwsRegion(region.toString())
              .build();

            AWSUtils.update(data.configuration, backupVault);

            discoverVaultTags(client, backupVault, data);

            emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":backupVault"), data.toJsonNode()));
          });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverVaultTags(BackupClient client, BackupVaultListMember resource, MagpieAwsResource data) {
    final String keyname = "Tags";
    getAwsResponse(
      () -> client.listTags(ListTagsRequest.builder().resourceArn(resource.backupVaultArn()).build()),
      (resp) -> data.supplementaryConfiguration = AWSUtils.update(
        data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> data.supplementaryConfiguration = AWSUtils.update(
        data.supplementaryConfiguration, Map.of(keyname, noresp))
    );

  }


  private void getPlanDetails(BackupClient client, MagpieAwsResource data) {
    getAwsResponse(
      () -> client.getBackupPlan(GetBackupPlanRequest.builder().backupPlanId(data.resourceId).build()),
      (resp) -> AWSUtils.update(data.configuration, resp),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, noresp)
    );
  }

  private void discoverTags(BackupClient client, BackupPlansListMember resource, MagpieAwsResource data, ObjectMapper mapper) {
    final String keyname = "Tags";
    getAwsResponse(
      () -> client.listTagsPaginator(ListTagsRequest.builder().resourceArn(resource.backupPlanArn()).build())
        .stream()
        .flatMap(r -> r.tags().entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
      (resp) -> data.tags = mapper.convertValue(resp, JsonNode.class),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }
}
