package io.openraven.magpie.plugins.aws.discovery;

import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.protocol.Message;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;

public class DiscoveryExceptions {

  private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryExceptions.class);

  static public void onDiscoveryException(String resourceType, String resourceName, Region region, AwsServiceException exception) {
    var event = createEvent(resourceType + " AwsServiceException", SentryLevel.WARNING);
    handleOrReportError(event, resourceType, resourceName, region, exception);
  }

  static public void onDiscoveryException(String resourceType, String resourceName, Region region, SdkServiceException exception) {
    handleOrReportError(createEvent(resourceType + " SdkServiceException",SentryLevel.ERROR), resourceType, resourceName, region, exception);
  }

  static public void onDiscoveryException(String resourceType, String resourceName, Region region, SdkException exception) {
    handleOrReportError(createEvent(resourceType + " SdkException",
      SentryLevel.ERROR), resourceType, resourceName, region, exception);
  }

  static public void onDiscoveryException(String resourceType, String resourceName, Region region, Exception exception) {
    handleOrReportError(createEvent(resourceType + " Exception",
      SentryLevel.ERROR), resourceType, resourceName, region, exception);
  }

  private static void handleOrReportError(SentryEvent sentryEvent, String resourceType, String resourceName, Region region, Exception exception) {
    if (!isManagedSdkException(resourceType, resourceName, exception)) {
      logErrorAndReportToSentry(resourceType, resourceName, region, sentryEvent, exception);
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
                                                SentryEvent sentryEvent, Exception exception) {
    LOGGER.error("{} - " + exception.getClass().getTypeName() + " on {} in {}, with error {}", resourceType, resourceName, region, exception.getMessage());
    sentryEvent.setExtra("Resource", String.valueOf(resourceType));
    sentryEvent.setThrowable(exception);
    Sentry.captureEvent(sentryEvent);
  }

  private static SentryEvent createEvent(String messageString, SentryLevel warning) {
      var result = new SentryEvent();
      result.setLevel(warning);
      var message = new Message();
      message.setMessage(messageString);
      result.setMessage(message);
      return result;
  }
}
