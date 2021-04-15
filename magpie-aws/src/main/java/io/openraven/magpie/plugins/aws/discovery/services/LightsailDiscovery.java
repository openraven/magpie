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
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;
import static io.openraven.magpie.plugins.aws.discovery.Conversions.GibToBytes;

public class LightsailDiscovery implements AWSDiscovery {

  private static final String SERVICE = "lightsail";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return LightsailClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    final var client = LightsailClient.builder().region(region).build();

    discoverDatabases(mapper, session, region, emitter, logger, client, account);
    discoverInstances(mapper, session, region, emitter, logger, client, account);
    discoverLoadBalancers(mapper, session, region, emitter, logger, client, account);
  }

  private void discoverDatabases(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, LightsailClient client, String account) {
    getAwsResponse(
      () -> client.getRelationalDatabases(GetRelationalDatabasesRequest.builder().build()).relationalDatabases(),
      (resp) -> resp.forEach(relationalDatabase -> {
        var data = new AWSResource(relationalDatabase.toBuilder(), region.toString(), account, mapper);
        data.arn = relationalDatabase.arn();
        data.resourceName = relationalDatabase.name();
        data.resourceType = "AWS::Lightsail::Database";

        discoverSize(client, relationalDatabase, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":relationalDatabase"), data.toJsonNode(mapper)));
      }),
      (noresp) -> logger.error("Failed to get relationalDatabases in {}", region)
    );
  }

  private void discoverSize(LightsailClient client, RelationalDatabase resource, AWSResource data) {
    var request = GetRelationalDatabaseMetricDataRequest.builder().
      relationalDatabaseName(resource.name()).
      metricName("FreeStorageSpace").
      period(60).
      startTime(Instant.now().minus(1, ChronoUnit.MINUTES)).
      endTime(Instant.now()).
      unit("Bytes").
      statistics(MetricStatistic.MINIMUM).
      build();
    var response = client.getRelationalDatabaseMetricData(request);

    long diskSizeInBytes = GibToBytes(resource.hardware().diskSizeInGb());

    if (!response.metricData().isEmpty()) {
      AWSUtils.update(data.supplementaryConfiguration, Map.of("FreeStorageSpace", response.metricData().get(0).minimum().longValue()));
      AWSUtils.update(data.supplementaryConfiguration, Map.of("SizeInBytes", diskSizeInBytes - response.metricData().get(0).minimum().longValue()));
      AWSUtils.update(data.supplementaryConfiguration, Map.of("DiskSizeInBytes", diskSizeInBytes));

      data.sizeInBytes = diskSizeInBytes - response.metricData().get(0).minimum().longValue();
    }
    data.maxSizeInBytes = diskSizeInBytes;
  }

  private void discoverInstances(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, LightsailClient client, String account) {
    getAwsResponse(
      () -> client.getInstances(GetInstancesRequest.builder().build()).instances(),
      (resp) -> resp.forEach(instance -> {
        var data = new AWSResource(instance.toBuilder(), region.toString(), account, mapper);
        data.arn = instance.arn();
        data.resourceName = instance.name();
        data.resourceType = "AWS::Lightsail::Instance";

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":instance"), data.toJsonNode(mapper)));
      }),
      (noresp) -> logger.error("Failed to get instances in {}", region)
    );
  }

  private void discoverLoadBalancers(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, LightsailClient client, String account) {
    getAwsResponse(
      () -> client.getLoadBalancers().loadBalancers(),
      (resp) -> resp.forEach(loadBalancer -> {
        var data = new AWSResource(loadBalancer.toBuilder(), region.toString(), account, mapper);
        data.arn = loadBalancer.arn();
        data.resourceName = loadBalancer.name();
        data.resourceType = "AWS::Lightsail::LoadBalancer";

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":loadBalancer"), data.toJsonNode(mapper)));
      }),
      (noresp) -> logger.error("Failed to get loadBalancers in {}", region)
    );
  }
}