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
import io.openraven.magpie.api.MagpieAwsResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.aws.elasticache.ElastiCacheCluster;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.javatuples.Pair;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.elasticache.ElastiCacheClient;
import software.amazon.awssdk.services.elasticache.model.CacheCluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getCloudwatchDoubleMetricMaximum;
import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getCloudwatchMetricMaximum;

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
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {
    final  String RESOURCE_TYPE = ElastiCacheCluster.RESOURCE_TYPE;

    try (final var client = clientCreator.apply(ElastiCacheClient.builder()).build()) {
      client.describeCacheClusters().cacheClusters().forEach(cacheCluster -> {
        var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, cacheCluster.arn())
          .withResourceName(cacheCluster.cacheClusterId())
          .withResourceId(cacheCluster.cacheClusterId())
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(cacheCluster.toBuilder()))
          .withAccountId(account)
          .withAwsRegion(region.toString())
          .build();

        discoverRedisSize(cacheCluster, data, region.id(), clientCreator);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":cacheCluster"), data.toJsonNode()));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverRedisSize(CacheCluster resource, MagpieAwsResource data, String region, MagpieAWSClientCreator clientCreator) {
    List<Dimension> dimensions = new ArrayList<>();
    dimensions.add(Dimension.builder().name("CacheClusterId").value(resource.cacheClusterId()).build());

    Pair<Long, GetMetricStatisticsResponse> volumeBytesUsed =
      getCloudwatchMetricMaximum(region, "AWS/ElastiCache", "BytesUsedForCache", dimensions, clientCreator);

    Pair<Double, GetMetricStatisticsResponse> DatabaseMemoryUsagePercentage =
      getCloudwatchDoubleMetricMaximum(region, "AWS/ElastiCache", "DatabaseMemoryUsagePercentage", dimensions, clientCreator);

    if (volumeBytesUsed.getValue0() != null) {
      AWSUtils.update(data.supplementaryConfiguration, Map.of(
        "BytesUsedForCache", volumeBytesUsed.getValue0(),
        "DatabaseMemoryUsagePercentage", DatabaseMemoryUsagePercentage.getValue0(),
        "DatabaseMaxSize", (long) (volumeBytesUsed.getValue0() / (DatabaseMemoryUsagePercentage.getValue0() / 100))));
    }
  }
}
