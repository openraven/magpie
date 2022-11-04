package io.openraven.magpie.plugins.gcp.discovery.exception;

import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscoveryExceptions {

  private static final Logger logger = LoggerFactory.getLogger(DiscoveryExceptions.class);

  static public void onDiscoveryException(String resourceType, Exception ex) {
    logger.error("{} - Exception , with error {}", resourceType, ex.getMessage());
    final var event = new SentryEvent();
    event.setLevel(SentryLevel.WARNING);
    final var message = new Message();
    message.setMessage(resourceType + ":" + ex.getMessage());
    event.setMessage(message);
    Sentry.captureEvent(event);
  }
}
