package io.openraven.magpie.plugins.gdrive.discovery.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.CredentialsProvider;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gdrive.discovery.GDriveDiscoveryPlugin;
import org.slf4j.Logger;

import java.util.Optional;

public interface GDriveDiscovery {
  String service();

  default void discoverWrapper(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger, Optional<CredentialsProvider> maybeCredentialsProvider) {
    logger.debug("Starting {} discovery ", service());
    discover(mapper, projectId, session, emitter, logger, maybeCredentialsProvider);
    logger.debug("Completed {} discovery", service());
  }

  void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger, Optional<CredentialsProvider> maybeCredentialsProvider);

  default String fullService() {
    return GDriveDiscoveryPlugin.ID + ":" + service();
  }

}
