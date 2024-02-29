/*
 * Copyright 2024 Open Raven Inc
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
package io.openraven.magpie.plugins.azure.discovery.services;

import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.Subscription;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieAzureResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.azure.discovery.AzureUtils;
import io.openraven.magpie.plugins.azure.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorageBlobDiscovery implements AzureDiscovery{

  private static final String SERVICE = "storage";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Emitter emitter, Logger logger, String subscriptionID, AzureResourceManager azrm, AzureProfile profile) {
    logger.info("Discovering storage");

    discoverStorageAccounts(mapper, session, emitter, logger, subscriptionID, azrm, profile);

  }

  private void discoverStorageAccounts(ObjectMapper mapper, Session session, Emitter emitter, Logger logger, String subscriptionID, AzureResourceManager azrm, AzureProfile profile) {


      azrm.storageAccounts().list().forEach(sa -> {
        try {
          final var resourceType = fullService() + ":storageAccount";
            final Subscription currentSubscription = azrm.getCurrentSubscription();
            final var data = new MagpieAzureResource.MagpieAzureResourceBuilder(mapper, sa.id())
            .withRegion(sa.regionName())
            .withResourceType(resourceType)
            .withCreatedIso(sa.creationTime().toInstant())
            .withResourceName(sa.name())
            .withTags(mapper.valueToTree(sa.tags()))
            .withUpdatedIso(Instant.now())
            .withsubscriptionId(subscriptionID)
            .withConfiguration(mapper.valueToTree(sa.innerModel()))
            .withContainingEntity(currentSubscription.displayName())
            .withContainingEntityId(currentSubscription.subscriptionId())
            .build();

            final var accountStatuses = new HashMap<String, String>();
            accountStatuses.put("primary", sa.accountStatuses().primary().toString());
            accountStatuses.put("secondary", sa.accountStatuses().secondary() == null ? "none" : sa.accountStatuses().secondary().toString());

            AzureUtils.update(data.supplementaryConfiguration, Map.of(
              "ipAddressesWithAccess", sa.ipAddressesWithAccess(),
              "accountStatuses", accountStatuses,
              "accessTier", sa.accessTier(),
              "encryptionStatuses", sa.encryptionStatuses(),
              "encryptionKeySource", sa.encryptionKeySource(),
              "provisioningState", sa.provisioningState(),
              "isBlobPublicAccessAllowed", sa.isBlobPublicAccessAllowed(),
              "isAccessAllowedFromAllNetworks", sa.isAccessAllowedFromAllNetworks(),
              "isHttpsTrafficOnly", sa.isHttpsTrafficOnly(),
//              "isSharedKeyAccessAllowed", sa.isSharedKeyAccessAllowed(),
              "isHnsEnabled", sa.isHnsEnabled()
            ));
          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(resourceType), data.toJsonNode()));
      } catch (Exception ex) {
        logger.warn("Exception during StorageAccount discovery", ex);
      }


      });
  }
}
