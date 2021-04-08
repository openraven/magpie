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
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.VersioningEmitterWrapper;
import org.javatuples.Pair;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.elasticache.ElastiCacheClient;
import software.amazon.awssdk.services.elasticache.model.CacheCluster;

import java.util.ArrayList;
import java.util.List;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.*;

public class ElastiCacheDiscovery implements AWSDiscovery {

  private static final String SERVICE = "elastiCache";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return ElastiCacheClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, VersioningEmitterWrapper emitter, Logger logger) {
    final var client = ElastiCacheClient.builder().region(region).build();

    getAwsResponse(
      () -> client.describeCacheClusters().cacheClusters(),
      (resp) -> resp.forEach(cacheCluster -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", cacheCluster.toBuilder());
        data.put("region", region.toString());

        discoverRedisSize(cacheCluster, data, region.id());

        emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":cacheCluster"), data));
      }),
      (noresp) -> logger.error("Failed to get cacheClusters in {}", region)
    );
  }

  private void discoverRedisSize(CacheCluster resource, ObjectNode data, String region) {
    List<Dimension> dimensions = new ArrayList<>();
    dimensions.add(Dimension.builder().name("CacheClusterId").value(resource.cacheClusterId()).build());

    Pair<Long, GetMetricStatisticsResponse> volumeBytesUsed =
      getCloudwatchMetricMaximum(region, "AWS/ElastiCache", "BytesUsedForCache", dimensions);

    Pair<Double, GetMetricStatisticsResponse> DatabaseMemoryUsagePercentage =
      getCloudwatchDoubleMetricMaximum(region, "AWS/ElastiCache", "DatabaseMemoryUsagePercentage", dimensions);

    if (volumeBytesUsed.getValue0() != null) {

      data.put("bytesUsedForCache", volumeBytesUsed.getValue0());
      data.put("databaseMemoryUsagePercentage", DatabaseMemoryUsagePercentage.getValue0());
      data.put("databaseMaxSize", (long) (volumeBytesUsed.getValue0() / (DatabaseMemoryUsagePercentage.getValue0() / 100)));
    }
  }
}
