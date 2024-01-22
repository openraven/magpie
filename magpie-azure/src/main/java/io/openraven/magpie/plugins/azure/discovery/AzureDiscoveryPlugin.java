package io.openraven.magpie.plugins.azure.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.OriginPlugin;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.azure.discovery.services.AzureDiscovery;
import io.openraven.magpie.plugins.azure.discovery.services.StorageBlobDiscovery;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class AzureDiscoveryPlugin implements OriginPlugin<AzureDiscoveryConfig> {

  public final static String ID = "magpie.azure.discovery";
  protected static final ObjectMapper MAPPER = new ObjectMapper()
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
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
    return null;
  }

  @Override
  public void discover(Session session, Emitter emitter) {
    final var enabledPlugins = DISCOVERY_LIST.stream().filter(p -> isEnabled(p.service())).collect(Collectors.toList());
  }

  private boolean isEnabled(String svc) {
    var enabled = config.getServices().isEmpty() || config.getServices().stream().anyMatch(configuredService -> configuredService.equalsIgnoreCase(svc));
    logger.debug("{} {} per config", enabled ? "Enabling" : "Disabling", svc);
    return enabled;
  }
}
