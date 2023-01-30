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

  default void discoverWrapper(ObjectMapper mapper, Session session, Emitter emitter, Logger logger, String projectId) {
    logger.debug("Starting {} discovery ", service());
    discover(mapper, session, emitter, logger, projectId);
    logger.debug("Completed {} discovery", service());
  }

  void discover(ObjectMapper mapper, Session session, Emitter emitter, Logger logger, String projectId);

  default String fullService() {
    return GDriveDiscoveryPlugin.ID + ":" + service();
  }

}
