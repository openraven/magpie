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
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieAwsResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.aws.cloudwatch.CloudWatchAlarm;
import io.openraven.magpie.data.aws.cloudwatch.CloudWatchDashboard;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;

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
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {
    try (final var client = clientCreator.apply(CloudWatchClient.builder()).build()) {
      discoverAlarms(mapper, session, region, emitter, client, account);
      discoverDashboards(mapper, session, region, emitter, client, account);
    }
  }

  private void discoverAlarms(ObjectMapper mapper, Session session, Region region, Emitter emitter, CloudWatchClient client, String account) {
    final String RESOURCE_TYPE = CloudWatchAlarm.RESOURCE_TYPE;

    try {
      client.describeAlarmsPaginator().metricAlarms().stream().forEach(alarm -> {
        var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, alarm.alarmArn())
          .withResourceName(alarm.alarmName())
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(alarm.toBuilder()))
          .withAccountId(account)
          .withAwsRegion(region.toString())
          .build();

        discoverAlarmHistory(client, alarm, data);
        discoverAlarmTags(client, alarm, data, mapper);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":alarm"), data.toJsonNode()));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverAlarmHistory(CloudWatchClient client, MetricAlarm resource, MagpieAwsResource data) {
    final String keyname = "alarmHistory";

    getAwsResponse(
      () -> client.describeAlarmHistoryPaginator(DescribeAlarmHistoryRequest.builder().alarmName(resource.alarmName()).build())
        .stream()
        .map(DescribeAlarmHistoryResponse::toBuilder)
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverAlarmTags(CloudWatchClient client, MetricAlarm resource, MagpieAwsResource data, ObjectMapper mapper) {
    getAwsResponse(
      () -> client.listTagsForResource(ListTagsForResourceRequest.builder().resourceARN(resource.alarmArn()).build()),
      (resp) -> {
        JsonNode tagsNode = mapper.convertValue(resp.tags().stream()
          .collect(Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
        AWSUtils.update(data.tags, tagsNode);
      },
      (noresp) -> AWSUtils.update(data.tags, noresp)
    );
  }


  private void discoverDashboards(ObjectMapper mapper, Session session, Region region, Emitter emitter, CloudWatchClient client, String account) {
    final String RESOURCE_TYPE = CloudWatchDashboard.RESOURCE_TYPE;
    try {
      client.listDashboardsPaginator(ListDashboardsRequest.builder().build()).dashboardEntries().stream()
        .forEach(dashboard -> {
        var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, dashboard.dashboardArn())
          .withResourceName(dashboard.dashboardName())
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(dashboard.toBuilder()))
          .withAccountId(account)
          .withAwsRegion(region.toString())
          .build();

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":dashboard"), data.toJsonNode()));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }
}
