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
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.api.Session;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.awssdk.services.lightsail.model.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger) {
    final var client = LightsailClient.builder().region(region).build();

    discoverDatabases(mapper, session, region, emitter, logger, client);
    discoverInstances(mapper, session, region, emitter, logger, client);
    discoverLoadBalancers(mapper, session, region, emitter, logger, client);
  }

  private void discoverDatabases(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, LightsailClient client) {
    getAwsResponse(
      () -> client.getRelationalDatabases(GetRelationalDatabasesRequest.builder().build()).relationalDatabases(),
      (resp) -> resp.forEach(relationalDatabase -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", relationalDatabase.toBuilder());
        data.put("region", region.toString());

        discoverSize(client, relationalDatabase, data);

        emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":relationalDatabase"), data));
      }),
      (noresp) -> logger.error("Failed to get relationalDatabases in {}", region)
    );
  }

  private void discoverSize(LightsailClient client, RelationalDatabase resource, ObjectNode data) {
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

    data.put("maxSizeInBytes", diskSizeInBytes);

    if (!response.metricData().isEmpty()) {
      data.put("FreeStorageSpace", response.metricData().get(0).minimum().longValue());
      data.put("SizeInBytes", diskSizeInBytes - response.metricData().get(0).minimum().longValue());
    }
  }

  private void discoverInstances(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, LightsailClient client) {
    getAwsResponse(
      () -> client.getInstances(GetInstancesRequest.builder().build()).instances(),
      (resp) -> resp.forEach(instance -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", instance.toBuilder());
        data.put("region", region.toString());

        emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":instance"), data));
      }),
      (noresp) -> logger.error("Failed to get instances in {}", region)
    );
  }

  private void discoverLoadBalancers(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, LightsailClient client) {
    getAwsResponse(
      () -> client.getLoadBalancers().loadBalancers(),
      (resp) -> resp.forEach(loadBalancer -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", loadBalancer.toBuilder());
        data.put("region", region.toString());

        emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":loadBalancer"), data));
      }),
      (noresp) -> logger.error("Failed to get loadBalancers in {}", region)
    );
  }
}
