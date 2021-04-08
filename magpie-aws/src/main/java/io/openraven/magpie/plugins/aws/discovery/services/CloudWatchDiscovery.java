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

package io.openraven.magpie.plugins.aws.discovery.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.VersioningEmitterWrapper;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class CloudWatchDiscovery implements AWSDiscovery {

  private static final String SERVICE = "cloudWatch";
  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return CloudWatchClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, VersioningEmitterWrapper emitter, Logger logger) {
    final var client = CloudWatchClient.builder().region(region).build();

    discoverAlarms(mapper, session, region, emitter, logger, client);
    discoverInsightRules(mapper, session, region, emitter, logger, client);
  }

  private void discoverAlarms(ObjectMapper mapper, Session session, Region region, VersioningEmitterWrapper emitter, Logger logger, CloudWatchClient client) {
    getAwsResponse(
      () -> client.describeAlarmsPaginator().metricAlarms().stream(),
      (resp) -> resp.forEach(alarm -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", alarm.toBuilder());
        data.put("region", region.toString());

        discoverAlarmHistory(client, alarm, data);
        discoverAlarmTags(client, alarm, data, mapper);

        emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":alarm"), data));
      }),
      (noresp) -> logger.error("Failed to get alarms in {}", region)
    );
  }

  private void discoverAlarmHistory(CloudWatchClient client, MetricAlarm resource, ObjectNode data) {
    final String keyname = "alarmHistory";

    getAwsResponse(
      () -> client.describeAlarmHistoryPaginator(DescribeAlarmHistoryRequest.builder().alarmName(resource.alarmName()).build())
        .stream()
        .map(r -> r.toBuilder())
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private void discoverAlarmTags(CloudWatchClient client, MetricAlarm resource, ObjectNode data, ObjectMapper mapper) {
    var obj = data.putObject("tags");

    getAwsResponse(
      () -> client.listTagsForResource(ListTagsForResourceRequest.builder().resourceARN(resource.alarmArn()).build()),
      (resp) -> {
        JsonNode tagsNode = mapper.convertValue(resp.tags().stream()
          .collect(Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
        AWSUtils.update(obj, tagsNode);
      },
      (noresp) -> AWSUtils.update(obj, noresp)
    );
  }


  private void discoverInsightRules(ObjectMapper mapper, Session session, Region region, VersioningEmitterWrapper emitter, Logger logger, CloudWatchClient client) {
    getAwsResponse(
      () -> client.describeInsightRulesPaginator(DescribeInsightRulesRequest.builder().build()).stream(),
      (resp) -> resp.forEach(insightRule -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", insightRule.toBuilder());
        data.put("region", region.toString());

        emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":insightRule"), data));
      }),
      (noresp) -> logger.error("Failed to get insightRules in {}", region)
    );
  }
}
