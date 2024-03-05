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

import com.azure.core.management.exception.ManagementException;
import com.azure.core.management.implementation.serializer.AzureJacksonAdapter;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.Subscription;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieAzureResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.azure.discovery.AzureUtils;
import io.openraven.magpie.plugins.azure.discovery.VersionedMagpieEnvelopeProvider;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class StorageDiscovery implements AzureDiscovery{

  private static final String SERVICE = "storage";

  private static final Set<Pattern> STORAGE_ACCOUNT_REFLECTION_INTERESTS = Set.of(
    Pattern.compile("^is.+"),
    Pattern.compile("^list.+"),
    Pattern.compile("^encryptionStatuses$"),
    Pattern.compile("^encryptionKeySource$"),
    Pattern.compile("^accessTier$"),
    Pattern.compile("^provisioningState$")
  );

  private static final Set<Pattern> STORAGE_ACCOUNT_REFLECTION_NON_INTERESTS = Set.of(
    Pattern.compile("listPrivateLinkResources")
  );

  private static final Set<Pattern> SBC_REFLECTION_INTERESTS = Set.of(
    Pattern.compile("^is.+"),
    Pattern.compile("^list.+"),
    Pattern.compile("^has.+"),
    Pattern.compile("^lease.+")
  );

  private static final Set<Pattern> SBC_REFLECTION_NON_INTERESTS = Set.of();

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Emitter emitter, Logger logger, String subscriptionID, AzureResourceManager azrm, AzureProfile profile) {
    logger.info("Discovering storage");

    final var triples = discoverStorageAccounts(mapper, session, emitter, logger, subscriptionID, azrm, profile);
    discoverStorageContainers(mapper, session, emitter, logger, subscriptionID, azrm, profile, triples);
  }

  private void discoverStorageContainers(ObjectMapper mapper, Session session, Emitter emitter, Logger logger, String subscriptionID, AzureResourceManager azrm, AzureProfile profile, List<Triple<String, String, String>> triples) {

    final var resourceType = fullService() + ":storageBlobContainer";
    final Subscription currentSubscription = azrm.getCurrentSubscription();

    triples.forEach(singleTuple ->
    {
      azrm.storageBlobContainers().list(singleTuple.getLeft(), singleTuple.getMiddle()).forEach(sbc ->
      {
        final var data = new MagpieAzureResource.MagpieAzureResourceBuilder(mapper, sbc.id())
          .withResourceType(resourceType)
          .withResourceName(sbc.name())
          .withUpdatedIso(Instant.now())
          .withsubscriptionId(subscriptionID)
          .withConfiguration(mapper.valueToTree(sbc))
          .withContainingEntity(currentSubscription.displayName())
          .withContainingEntityId(currentSubscription.subscriptionId())
          .build();

          final var props = AzureUtils.reflectProperties(sbc.id(), sbc, SBC_REFLECTION_INTERESTS, SBC_REFLECTION_NON_INTERESTS, logger, mapper);
          AzureUtils.update(data.supplementaryConfiguration, Map.of("properties", props));

          AzureUtils.update(data.supplementaryConfiguration, Map.of("location", Map.of(
            "resourceGroupName", singleTuple.getLeft(),
            "storageAccountName", singleTuple.getMiddle(),
            "storageAccountEndpoint", singleTuple.getRight()
          )));

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(resourceType), data.toJsonNode()));
      });
    });
  }

  private List<Triple<String, String, String>> discoverStorageAccounts(ObjectMapper mapper, Session session, Emitter emitter, Logger logger, String subscriptionID, AzureResourceManager azrm, AzureProfile profile) {

    final List<Triple<String, String, String>> resourceGroupToAccountNameTuples = new ArrayList<>();

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
        AzureUtils.update(data.supplementaryConfiguration, Map.of("accountStatuses", accountStatuses));

        final var keyMap =  new HashMap<String, String>();
        wrappedCall(() -> sa.getKeys().forEach(k -> keyMap.put(k.keyName(), k.value())));
        AzureUtils.update(data.supplementaryConfiguration, Map.of("keys", keyMap));

        final var props = AzureUtils.reflectProperties(sa.id(), sa, STORAGE_ACCOUNT_REFLECTION_INTERESTS, STORAGE_ACCOUNT_REFLECTION_NON_INTERESTS, logger, mapper);
        AzureUtils.update(data.supplementaryConfiguration, Map.of("properties", props));
        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(resourceType), data.toJsonNode()));

        resourceGroupToAccountNameTuples.add(Triple.of(sa.resourceGroupName(), sa.name(), sa.endPoints().primary().blob()));
      } catch (Exception ex) {
        logger.warn("Exception during StorageAccount discovery", ex);
      }
    });

    return resourceGroupToAccountNameTuples;
  }

  public <T> T wrappedCall(Supplier<T> fetch) {
    try {
      return fetch.get();
    } catch(ManagementException managementException) {
      if ("AuthorizationFailed".equals(managementException.getValue().getCode())){
        return null;
      }
      throw new RuntimeException(managementException);
    }
  }
  public void wrappedCall(Runnable runnable) {
    try {
      runnable.run();
    } catch(ManagementException managementException) {
      if ("AuthorizationFailed".equals(managementException.getValue().getCode())){
        return;
      }
      throw new RuntimeException(managementException);
    }
  }
}
