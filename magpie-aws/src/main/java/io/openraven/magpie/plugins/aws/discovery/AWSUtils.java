/*
 * Copyright 2021 Open Raven Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openraven.magpie.plugins.aws.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

public class AWSUtils {

  private static final JsonNode NULL_NODE = AWSDiscoveryPlugin.MAPPER.nullNode();

  private static final Logger LOGGER = LoggerFactory.getLogger(AWSUtils.class);

  /**
   * @param resp will be provided the output from calling {@code fn}, or @param noresp a {@code NullNode} in the 403 or 404 case
   * @throws SdkServiceException if it is not one of the 403 or 404 status codes
   */
  public static <R> void getAwsResponse(Supplier<R> fn, Consumer<R> resp, Consumer<JsonNode> noresp) throws SdkClientException, SdkServiceException {
    try {
      R ret = fn.get();
      resp.accept(ret);
    }
    catch (SdkServiceException ex) {
      if (ex.statusCode() >= 400 && ex.statusCode() < 500) {
        noresp.accept(NULL_NODE);
      }
      else {
        throw ex;
      }
    }
  }

  public static JsonNode update(@Nullable JsonNode payload, ToCopyableBuilder... responsesToAdd) {
    for (ToCopyableBuilder responseToAdd : responsesToAdd) {
      if (responseToAdd != null) {
        JsonNode jsonNode = AWSDiscoveryPlugin.MAPPER.convertValue(responseToAdd.toBuilder(), JsonNode.class);
        payload = update(payload, jsonNode);
      }
    }
    return payload;
  }

  @SuppressWarnings("rawtypes")
  public static JsonNode update(@Nullable JsonNode payload,
                                Map<String, Object> mappedResponsesToAdd) {

    for (Map.Entry<String, Object> responseToAdd : mappedResponsesToAdd.entrySet()) {
      ObjectNode nodeToAdd = AWSDiscoveryPlugin.MAPPER.createObjectNode();

      if (responseToAdd.getValue() instanceof ToCopyableBuilder) {
        nodeToAdd.set(responseToAdd.getKey(),
          AWSDiscoveryPlugin.MAPPER.convertValue(((ToCopyableBuilder) responseToAdd.getValue()).toBuilder(),
            JsonNode.class));
      } else {
        nodeToAdd.set(responseToAdd.getKey(),
          AWSDiscoveryPlugin.MAPPER.convertValue(responseToAdd.getValue(), JsonNode.class));
      }

      payload = update(payload, nodeToAdd);
    }

    return payload;
  }

  public static JsonNode update(@Nullable JsonNode payload, JsonNode... nodesToAdd) {
    for (JsonNode nodeToAdd : nodesToAdd) {
      if (nodeToAdd != null) {
        try {
          if (payload != null) {
            payload = AWSDiscoveryPlugin.MAPPER.readerForUpdating(payload).readValue(nodeToAdd);
          } else {
            payload = nodeToAdd;
          }
        } catch (IOException e) {
          LOGGER.warn("Unable to add extra data {}", nodeToAdd, e);
        }
      }
    }

    return payload;
  }

  @SuppressWarnings("rawtypes")
  public static JsonNode update(ToCopyableBuilder... responsesToAdd) {
    return update(null, responsesToAdd);
  }

  public static JsonNode add(List<? extends ToCopyableBuilder> responsesToAdd) {
    List<JsonNode> tags = responsesToAdd.stream()
      .map((val) -> AWSDiscoveryPlugin.MAPPER.convertValue(val.toBuilder(), JsonNode.class))
      .collect(toList());

    ArrayNode payload = AWSDiscoveryPlugin.MAPPER.createArrayNode();
    payload.addAll(tags);

    return payload;
  }

  public static Pair<Long, GetMetricStatisticsResponse> getCloudwatchMetricMaximum(
    String regionID, String namespace, String metric, List<Dimension> dimensions) {

    GetMetricStatisticsResponse getMetricStatisticsResult = getCloudwatchMetricStatistics(regionID, namespace, metric, Statistic.MAXIMUM, dimensions);

    return Pair.with(getMetricStatisticsResult.datapoints().stream().map(Datapoint::maximum)
      .map(Double::longValue).max(Long::compareTo).orElse(null), getMetricStatisticsResult);

  }

  public static GetMetricStatisticsResponse getCloudwatchMetricStatistics( String regionID, String namespace, String metric, Statistic statistic, List<Dimension> dimensions) {

    try (final CloudWatchClient client = CloudWatchClient.builder().region(Region.of(regionID)).build();) {

      // The start time is t-minus 2 days (48 hours) because an asset is considered "active" if it's been updated within
      // 48hrs, otherwise it is considered "terminated/deleted", so start capturing at the longest possible period
      // (even though should be discovering more frequently). TODO: maybe pull these constants out to config?
      Instant startTS = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MINUTES);

      // the end time is t-minus 1 hour to account for delay in some services pushing data to cloudwatch - metrics
      // earlier than this may not be available or unreliable (due to aggregations)
      Instant endTS = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MINUTES);

      GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder().startTime(startTS)
        .endTime(endTS)
        .namespace(namespace).period(3600).metricName(metric).statistics(statistic)
        .dimensions(dimensions).build();

      return client.getMetricStatistics(request);
    }
  }
}
