package io.openraven.magpie.plugins.aws.discovery;

import com.google.common.collect.ImmutableMap;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.TrackMessage;
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
  private static String magpieContext = "";
  private static Analytics segmentAnalytics = null;

  static public void configureAnalytics(String context, String key) {
    magpieContext = context;
    segmentAnalytics = Analytics.builder(key).build();
  }

  static public void onDiscoveryException(String resourceType, String resourceName, Region region, AwsServiceException ex, String discoverySession) {
    if (!handleKnownException(resourceType, resourceName, region, ex, discoverySession)) {
      logger.error("{} - AwsServiceException on {} in {}, with error {}", resourceType, resourceName, region, ex.getMessage());
      Sentry.capture(new EventBuilder().withMessage(resourceType + " AwsServiceException")
        .withLevel(Event.Level.WARNING)
        .withFingerprint(String.valueOf(resourceType), String.valueOf(ex.awsErrorDetails()))
        .withExtra("Resource", String.valueOf(resourceType))
        .withSentryInterface(new ExceptionInterface(ex)));
    }
  }

  static public void onDiscoveryException(String resourceType, String resourceName, Region region, SdkServiceException ex, String discoverySession) {
    if (!handleKnownException(resourceType, resourceName, region, ex, discoverySession)) {
      logger.error("{} - SdkServiceException on {} in {}, with error {}", resourceType, resourceName, region, ex.getMessage());
      Sentry.capture(new EventBuilder().withMessage(resourceType + " SdkServiceException")
        .withLevel(Event.Level.ERROR)
        .withExtra("Resource", String.valueOf(resourceType))
        .withSentryInterface(new ExceptionInterface(ex)));
    }
  }


  static public void onDiscoveryException(String resourceType, String resourceName, Region region, SdkException ex, String discoverySession) {
    if (!handleKnownException(resourceType, resourceName, region, ex, discoverySession)) {
      logger.error("{} - SdkException on {} in {}, with error {}", resourceType, resourceName, region, ex.getMessage());
      Sentry.capture(new EventBuilder().withMessage(resourceType + " SdkException")
        .withLevel(Event.Level.ERROR)
        .withExtra("Resource", String.valueOf(resourceType))
        .withSentryInterface(new ExceptionInterface(ex)));
    }
  }

  static public void onDiscoveryException(String resourceType, String resourceName, Region region, Exception ex, String discoverySession) {
    if (!handleKnownException(resourceType, resourceName, region, ex, discoverySession)) {
      logger.error("{} - Exception on {} in {}, with error {}", resourceType, resourceName, region, ex.getMessage());
      Sentry.capture(new EventBuilder().withMessage(resourceType + " Exception")
        .withLevel(Event.Level.ERROR)
        .withExtra("Resource", String.valueOf(resourceType))
        .withSentryInterface(new ExceptionInterface(ex)));
    }
  }

  static public boolean handleKnownException(String resourceType, String resourceName, Region region, Exception ex, String discoverySession) {
    if ((ex instanceof SdkServiceException) && ((SdkServiceException) ex).isThrottlingException()) {
      logger.warn("{} - Throttling exception on {} in {}, with error {}", resourceType, resourceName, region, ex.getMessage());
      var errorPropertyMap = ImmutableMap
        .of("discovery-session", discoverySession,
          "magpieContext", magpieContext,
          "resourceType", resourceType != null ? resourceType : "null",
          "functionOrResourceName", resourceName != null ? resourceName : "null",
          "exception", ex.toString());
      sendAnalyticsEvent("throttling-error", errorPropertyMap);
      return true;
    }

    if ((ex instanceof SdkServiceException) && ((SdkServiceException) ex).isClockSkewException()) {
      logger.warn("{} - Clock skew exception on {} in {}, with error {}", resourceType, resourceName, region, ex.getMessage());
      var errorPropertyMap = ImmutableMap
        .of("discovery-session", discoverySession,
          "magpieContext", magpieContext,
          "resourceType", resourceType != null ? resourceType : "null",
          "functionOrResourceName", resourceName != null ? resourceName : "null",
          "exception", ex.toString());
      sendAnalyticsEvent("clock-skew-error", errorPropertyMap);
      return true;
    }

    if ((ex instanceof SdkServiceException) && (((SdkServiceException) ex).statusCode() == 404)) {
      logger.warn("404 when accessing resource {}", resourceName, ex);
      // We don't really need to know about when these happen, so don't send any sentry or analytics events
      return true;
    }

    if (ex.getMessage().contains("STS is not activated in this region")) {
      logger.info("STS is not activated in this region for resource {}  in {}", resourceName, region, ex);
      var errorPropertyMap = ImmutableMap
        .of("discovery-session", discoverySession,
          "magpieContext", magpieContext,
          "resourceType", resourceType != null ? resourceType : "null",
          "functionOrResourceName", resourceName != null ? resourceName : "null",
          "exception", ex.toString());
      sendAnalyticsEvent("STS-region-disabled", errorPropertyMap);
      return true;
    }

    if (ex.getMessage().contains("not authorized to perform") ||
      ex.getMessage().contains("AccessDenied") ||
      ex.getMessage().contains("Access Denied")) {
      logger.info("Access denied on {}  in {}", resourceName, region, ex);
      var errorPropertyMap = ImmutableMap
        .of("discovery-session", discoverySession,
          "magpieContext", magpieContext,
          "resourceType", resourceType != null ? resourceType : "null",
          "functionOrResourceName", resourceName != null ? resourceName : "null",
          "exception", ex.toString());
      sendAnalyticsEvent("access-denied", errorPropertyMap);
      return true;
    }

    return false;
  }

  static public void sendAnalyticsEvent(String event, ImmutableMap<String, String> map) {
    logger.error("Sending analytics event {} with properties {}", event, map);

    if (segmentAnalytics != null) {
      segmentAnalytics.enqueue(TrackMessage.builder(event).anonymousId("0").properties(map));
    }
  }
}
