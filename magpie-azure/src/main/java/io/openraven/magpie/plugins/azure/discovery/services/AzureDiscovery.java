package io.openraven.magpie.plugins.azure.discovery.services;

import com.azure.core.credential.TokenCredential;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import org.slf4j.Logger;

public interface AzureDiscovery {

  void discover(ObjectMapper mapper, Session session, Emitter Emitter, Logger logger, TokenCredential credentials, String account);

  String service();
}
