package io.openraven.magpie.plugins.azure.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import org.slf4j.Logger;

import java.util.Map;

public interface AzureDiscovery {

  void discover(ObjectMapper mapper, Session session, Emitter Emitter, Logger logger, Map<String, Object> credentials, String account);

  String service();
}
