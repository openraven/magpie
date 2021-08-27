package io.openraven.magpie.plugins.gcp.discovery.exception;

import io.sentry.Sentry;
import io.sentry.event.Event;
import io.sentry.event.EventBuilder;
import io.sentry.event.interfaces.ExceptionInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscoveryExceptions {

  private static final Logger logger = LoggerFactory.getLogger(DiscoveryExceptions.class);

  static public void onDiscoveryException(String resourceType, Exception ex) {
    logger.error("{} - Exception , with error {}", resourceType, ex.getMessage());
    Sentry.capture(new EventBuilder().withMessage(resourceType + " Exception")
      .withLevel(Event.Level.WARNING)
      .withFingerprint(String.valueOf(resourceType), String.valueOf(ex.getMessage()))
      .withExtra("Resource", String.valueOf(resourceType))
      .withSentryInterface(new ExceptionInterface(ex)));
  }
}
