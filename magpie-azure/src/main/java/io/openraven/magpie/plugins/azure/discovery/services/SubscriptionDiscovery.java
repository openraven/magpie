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
import com.azure.resourcemanager.resources.models.Tenant;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieAzureResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.azure.subscriptions.Subscription;
import io.openraven.magpie.plugins.azure.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.List;

public class SubscriptionDiscovery implements AzureDiscovery {

  private static final String SERVICE = "subscriptions";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Emitter emitter, Logger logger, String subscriptionID, AzureResourceManager azrm, AzureProfile profile) {
    logger.info("Discovering subscriptions");

    discoverSubscriptions(mapper, session, emitter, logger, subscriptionID, azrm, profile);

  }

  private void discoverSubscriptions(ObjectMapper mapper, Session session, Emitter emitter, Logger logger, String subscriptionID, AzureResourceManager azrm, AzureProfile profile) {

    final var resourceType = Subscription.RESOURCE_TYPE;

    try {
      azrm.subscriptions().list().forEach(sa -> {
        var maybeTenantInfo = azrm.tenants().list().stream().filter(t -> t.tenantId().equals(azrm.tenantId())).map(Tenant::innerModel).findFirst();

        final var builder = new MagpieAzureResource.MagpieAzureResourceBuilder(mapper, sa.subscriptionId())
          .withResourceType(resourceType)
          .withResourceName(sa.displayName())
          .withUpdatedIso(Instant.now())
          .withsubscriptionId(subscriptionID)
          .withConfiguration(mapper.valueToTree(sa.innerModel()));
        maybeTenantInfo.ifPresent(inner -> {
          builder.withContainingEntity(inner.displayName());
          builder.withContainingEntityId(inner.tenantId());
        });
        var data = builder.build();
        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(resourceType), data.toJsonNode()));
      });
    } catch (Exception ex) {
      logger.warn("Exception during Subscription discovery", ex);
    }
  }
}
