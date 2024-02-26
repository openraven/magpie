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
package io.openraven.magpie.plugins.azure.discovery;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.core.util.Configuration;
import com.azure.core.util.ConfigurationBuilder;
import com.azure.identity.AzureCliCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.OriginPlugin;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.azure.discovery.services.AzureDiscovery;
import io.openraven.magpie.plugins.azure.discovery.services.StorageBlobDiscovery;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AzureDiscoveryPlugin implements OriginPlugin<AzureDiscoveryConfig> {

  public final static String ID = "magpie.azure.discovery";
  protected static final ObjectMapper MAPPER = new ObjectMapper()
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .enable(SerializationFeature.INDENT_OUTPUT)
    .findAndRegisterModules();

  private static final List<AzureDiscovery> DISCOVERY_LIST = List.of(
    new StorageBlobDiscovery()
  );

  private AzureDiscoveryConfig config;
  private Logger logger;

  @Override
  public String id() {
    return ID;
  }

  @Override
  public void init(AzureDiscoveryConfig config, Logger logger) {
    this.config = config;
    this.logger = logger;
  }

  @Override
  public Class<AzureDiscoveryConfig> configType() {
    return AzureDiscoveryConfig.class;
  }

  @Override
  public void discover(Session session, Emitter emitter) {

      final List<Map<String, Object>> fetchedCredentials = config.getCredentials();
      if(fetchedCredentials.isEmpty()) {
      final var azureCliCredential = new AzureCliCredentialBuilder().build();
      final var profile = new AzureProfile(AzureEnvironment.AZURE);
      final var azrm = AzureResourceManager
        .configure()
        .authenticate(azureCliCredential, profile)
        .withDefaultSubscription();
      final var subscriptionID = azrm.getCurrentSubscription().subscriptionId();

      discover(session, emitter, logger, subscriptionID, azrm, profile);
    } else {
      fetchedCredentials.forEach(mapOfKeysToCredsAndSubInfo -> {

        final var subscriptionID = (String)mapOfKeysToCredsAndSubInfo.get("subscription-id");
        final var creds = (TokenCredential) mapOfKeysToCredsAndSubInfo.get("creds");
        final var profile = new AzureProfile(AzureEnvironment.AZURE);
        final var azrm = AzureResourceManager
          .configure()
          .withConfiguration(new ConfigurationBuilder().putProperty(Configuration.PROPERTY_AZURE_REGIONAL_AUTHORITY_NAME, "westus3").build())
          .authenticate(creds, profile)
          .withSubscription(subscriptionID);

        discover(session, emitter, logger, subscriptionID, azrm, profile);
      });
    }
  }

  private void discover(Session session, Emitter emitter, Logger logger, String subscriptionID, AzureResourceManager azrm, AzureProfile profile) {
    DISCOVERY_LIST.stream().filter(p -> isEnabled(p.service())).collect(Collectors.toList()).forEach(plugin -> {
      try {
        plugin.discover(MAPPER, session, emitter, logger, subscriptionID, azrm, profile);
      } catch (Exception ex) {
        logger.error("Discovery failed for {}", plugin.service(), ex);
      }
    });
  }
  private boolean isEnabled(String svc) {
    var enabled = config.getServices().isEmpty() || config.getServices().stream().anyMatch(configuredService -> configuredService.equalsIgnoreCase(svc));
    logger.debug("{} {} per config", enabled ? "Enabling" : "Disabling", svc);
    return enabled;
  }
}
