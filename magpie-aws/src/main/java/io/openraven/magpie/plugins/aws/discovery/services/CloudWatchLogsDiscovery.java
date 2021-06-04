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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSResource;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;

import java.time.Instant;
import java.util.List;

public class CloudWatchLogsDiscovery implements AWSDiscovery {

  private static final String SERVICE = "cloudWatchLogs";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return CloudWatchClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    final var client = CloudWatchLogsClient.builder().region(region).build();

    discoverLogs(mapper, session, region, emitter, client, account);
  }

  private void discoverLogs(ObjectMapper mapper, Session session, Region region, Emitter emitter, CloudWatchLogsClient client, String account) {
    final String RESOURCE_TYPE = "AWS::CloudWatchLogs::MetricFilter";

    try {
      client.describeMetricFiltersPaginator().metricFilters().forEach(metricFilter -> {
        var data = new AWSResource(metricFilter.toBuilder(), region.toString(), account, mapper);
        data.arn = String.format("arn:aws:cloudwatchlogs:%s:%s:metric-filter/%s", region, account, metricFilter.filterName());
        data.resourceName = metricFilter.filterName();
        data.resourceType = RESOURCE_TYPE;
        data.createdIso = Instant.ofEpochSecond(metricFilter.creationTime().longValue());

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":metricFilter"), data.toJsonNode(mapper)));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }
}
