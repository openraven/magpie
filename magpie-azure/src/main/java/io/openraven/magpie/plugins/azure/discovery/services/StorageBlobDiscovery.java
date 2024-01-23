package io.openraven.magpie.plugins.azure.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import org.slf4j.Logger;

public class StorageBlobDiscovery implements AzureDiscovery{

  private static final String SERVICE = "storage";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Emitter Emitter, Logger logger, String account) {

  }
}
