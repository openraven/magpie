package io.openraven.magpie.plugins.aws.discovery;

import io.sentry.Sentry;
import io.sentry.event.Event;
import io.sentry.event.EventBuilder;
import io.sentry.event.interfaces.ExceptionInterface;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;

public class DiscoveryExceptions {

  private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryExceptions.class);

  static public void onDiscoveryException(String resourceType, String resourceName, Region region, AwsServiceException ex) {
    handleOrReportError(new EventBuilder().withMessage(resourceType + " AwsServiceException")
      .withLevel(Event.Level.WARNING), resourceType, resourceName, region, ex);
  }

  static public void onDiscoveryException(String resourceType, String resourceName, Region region, SdkServiceException ex) {
    handleOrReportError(new EventBuilder().withMessage(resourceType + " SdkServiceException")
      .withLevel(Event.Level.ERROR), resourceType, resourceName, region, ex);
  }

  static public void onDiscoveryException(String resourceType, String resourceName, Region region, SdkException ex) {
    handleOrReportError(new EventBuilder().withMessage(resourceType + " SdkException")
      .withLevel(Event.Level.ERROR), resourceType, resourceName, region, ex);
  }

  static public void onDiscoveryException(String resourceType, String resourceName, Region region, Exception ex) {
    handleOrReportError(new EventBuilder().withMessage(resourceType + " Exception")
      .withLevel(Event.Level.ERROR), resourceType, resourceName, region, ex);
  }

  private static void handleOrReportError(EventBuilder eventBuilder, String resourceType, String resourceName, Region region, Exception ex) {
    if (!isManagedSdkException(resourceType, resourceName, ex)) {
      logErrorAndReportToSentry(resourceType, resourceName, region, eventBuilder, ex);
    }
  }

  /*
   * returns true and logs given exception if the exception is an AWS SDK exception that is non-exceptional to Magpie
   */
  private static boolean isManagedSdkException(String resourceType, String resourceName, Exception exception) {
    if ((exception instanceof SdkServiceException) && (((SdkServiceException) exception).isThrottlingException())) {
      LOGGER.warn("{} - Throttling exception on {}, with error {}", resourceType, resourceName, exception.getMessage());
      return true;
    }

    if ((exception instanceof SdkServiceException) && ((SdkServiceException) exception).isClockSkewException()) {
      LOGGER.warn("{} - Clock skew exception on {}, with error {}", resourceType, resourceName, exception.getMessage());
      return true;
    }

    if ((exception instanceof SdkServiceException) && (((SdkServiceException) exception).statusCode() == HttpStatus.SC_NOT_FOUND)) {
      LOGGER.warn("404 when accessing resource {}", resourceName);
      return true;
    }

    if (exception.getMessage().contains("STS is not activated in this region")) {
      LOGGER.info("STS is not activated in this region for resource {}", resourceName, exception);
      return true;
    }

    if (exception.getMessage().contains("not authorized to perform") ||
      exception.getMessage().contains("AccessDenied") ||
      exception.getMessage().contains("Access Denied")) {
      LOGGER.info("Access denied on {}", resourceName);
      return true;
    }

    if (exception.getMessage().contains("The AWS Access Key Id needs a subscription for the service")) {
      LOGGER.info("The AWS Access Key Id needs a subscription for the service for resource {}", resourceName);
      return true;
    }
    return false;
  }

  private static void logErrorAndReportToSentry(String resourceType, String resourceName, Region region,
                                                EventBuilder eventBuilder, Exception exception) {
    LOGGER.error("{} - " + exception.getClass().getTypeName() + " on {} in {}, with error {}", resourceType, resourceName, region, exception.getMessage());
    Sentry.capture(eventBuilder
      .withExtra("Resource", String.valueOf(resourceType))
      .withSentryInterface(new ExceptionInterface(exception)));
  }

}
