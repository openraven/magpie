package io.openraven.magpie.plugins.aws.discovery;

import io.sentry.Sentry;
import io.sentry.event.Event;
import io.sentry.event.EventBuilder;
import io.sentry.event.interfaces.ExceptionInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;

public class DiscoveryExceptions {

  private static final Logger logger = LoggerFactory.getLogger(DiscoveryExceptions.class);

  static public void onDiscoveryException(String resourceType, String resourceName, Region region, AwsServiceException ex) {
    logger.error("{} - AwsServiceException on {} in {}, with error {}", resourceType, resourceName, region, ex.getMessage());
    Sentry.capture(new EventBuilder().withMessage(resourceType + " AwsServiceException")
      .withLevel(Event.Level.WARNING)
      .withFingerprint(String.valueOf(resourceType), String.valueOf(ex.awsErrorDetails()))
      .withExtra("Resource", String.valueOf(resourceType))
      .withSentryInterface(new ExceptionInterface(ex)));
  }

  static public void onDiscoveryException(String resourceType, String resourceName, Region region, SdkServiceException ex) {
    logger.error("{} - SdkServiceException on {} in {}, with error {}", resourceType, resourceName, region, ex.getMessage());
    Sentry.capture(new EventBuilder().withMessage(resourceType + " SdkServiceException")
      .withLevel(Event.Level.ERROR)
      .withExtra("Resource", String.valueOf(resourceType))
      .withSentryInterface(new ExceptionInterface(ex)));

  }


  static public void onDiscoveryException(String resourceType, String resourceName, Region region, SdkException ex) {
    logger.error("{} - SdkException on {} in {}, with error {}", resourceType, resourceName, region, ex.getMessage());
    Sentry.capture(new EventBuilder().withMessage(resourceType + " SdkException")
      .withLevel(Event.Level.ERROR)
      .withExtra("Resource", String.valueOf(resourceType))
      .withSentryInterface(new ExceptionInterface(ex)));
  }

  static public void onDiscoveryException(String resourceType, String resourceName, Region region, Exception ex) {
    logger.error("{} - Exception on {} in {}, with error {}", resourceType, resourceName, region, ex.getMessage());
    Sentry.capture(new EventBuilder().withMessage(resourceType + " Exception")
      .withLevel(Event.Level.ERROR)
      .withExtra("Resource", String.valueOf(resourceType))
      .withSentryInterface(new ExceptionInterface(ex)));
  }
}
