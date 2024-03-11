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
import com.azure.resourcemanager.resources.models.Tenant;
import com.azure.resourcemanager.sql.models.SqlServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieAzureResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.azure.sql.SQLDatabase;
import io.openraven.magpie.data.azure.sql.SQLServer;
import io.openraven.magpie.plugins.azure.discovery.AzureUtils;
import io.openraven.magpie.plugins.azure.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class SQLDiscovery implements AzureDiscovery {

  private static final String SERVICE = "sql";


  private static final Set<Pattern> DB_REFLECTION_INTERESTS = Set.of(
    Pattern.compile("^is.+"),
    Pattern.compile("^list.+"),
    Pattern.compile("^get.+"),
    Pattern.compile("^edition.*"),
    Pattern.compile("^elastic.+"),
    Pattern.compile("^status.*")
  );

  private static final Set<Pattern> DB_REFLECTION_NON_INTERESTS = Set.of(
    Pattern.compile("^getDatabaseAutomaticTuning")
  );

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Emitter emitter, Logger logger, String subscriptionID, AzureResourceManager azrm, AzureProfile profile) {
    logger.info("Discovering SQL");

    discoverServers(mapper, session, emitter, logger, azrm.getCurrentSubscription(), azrm, profile);
  }

  private void discoverServers(ObjectMapper mapper, Session session, Emitter emitter, Logger logger, Subscription subscription, AzureResourceManager azrm, AzureProfile profile) {

    final var resourceType = SQLServer.RESOURCE_TYPE;

    azrm.sqlServers().list().forEach(sql -> {
      try {
        final var data = new MagpieAzureResource.MagpieAzureResourceBuilder(mapper, sql.id())
          .withRegion(sql.regionName())
          .withResourceType(resourceType)
          .withResourceName(sql.name())
          .withTags(mapper.valueToTree(sql.tags()))
          .withUpdatedIso(Instant.now())
          .withsubscriptionId(subscription.subscriptionId())
          .withConfiguration(mapper.valueToTree(sql.innerModel()))
          .withContainingEntity(subscription.displayName())
          .withContainingEntityId(subscription.subscriptionId())
          .build();



        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(resourceType), data.toJsonNode()));
        discoverDatabases(mapper, session, emitter, logger, subscription, azrm, sql);
      } catch (Exception ex) {
        logger.warn("Exception during SQL Server discovery", ex);
      }
    });

  }

  private void discoverDatabases(ObjectMapper mapper, Session session, Emitter emitter, Logger logger, Subscription subscription, AzureResourceManager azrm, SqlServer server) {

    final var resourceType = SQLDatabase.RESOURCE_TYPE;

    server.databases().list().forEach(db -> {
      try {
        final var data = new MagpieAzureResource.MagpieAzureResourceBuilder(mapper, db.id())
          .withRegion(db.regionName())
          .withResourceType(resourceType)
          .withResourceName(db.name())
          .withCreatedIso(db.creationDate().toInstant())
          .withUpdatedIso(Instant.now())
          .withsubscriptionId(subscription.subscriptionId())
          .withConfiguration(mapper.valueToTree(db.innerModel()))
          .withContainingEntity(subscription.displayName())
          .withContainingEntityId(subscription.subscriptionId())
          .build();

        final var props = AzureUtils.reflectProperties(db.id(), db, DB_REFLECTION_INTERESTS, DB_REFLECTION_NON_INTERESTS, logger, mapper);
        AzureUtils.update(data.supplementaryConfiguration, Map.of("properties", props));

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(resourceType), data.toJsonNode()));
      } catch (Exception ex) {
        logger.warn("Exception during SQL DB discovery", ex);
      }
    });
  }
}
