/*
 * Copyright 2023 Open Raven Inc
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
import io.openraven.magpie.data.aws.quotas.ServiceQuota;
import io.openraven.magpie.plugins.aws.discovery.AWSDiscoveryConfig;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import io.openraven.magpie.plugins.aws.discovery.services.servicequotametrics.MetricCounter;
import io.openraven.magpie.plugins.aws.discovery.services.servicequotametrics.VpcsPerRegionCounter;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;
import software.amazon.awssdk.services.servicequotas.ServiceQuotasClient;
import software.amazon.awssdk.services.servicequotas.model.ListServiceQuotasRequest;
import software.amazon.awssdk.services.servicequotas.model.ListServicesRequest;
import software.amazon.awssdk.services.servicequotas.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.servicequotas.model.Tag;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServiceQuotaDiscovery implements AWSDiscovery {
  private static final String SERVICE = "servicequotas";


  private static final List<MetricCounter> COUNTERS = List.of(
    new VpcsPerRegionCounter()
  );


  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    var regions = new LinkedList<>(ServiceQuotasClient.serviceMetadata().regions());
    regions.add(Region.AWS_GLOBAL);
    return regions;
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator, AWSDiscoveryConfig config) {
    final var RESOURCE_TYPE = ServiceQuota.RESOURCE_TYPE;

    //
    // Global ServiceQuotas are only available when querying us-east-1. This means
    // 1) When the above region specified AWS Global, we need to substitute us-east-1 in the
    // client.
    // 2) When the specified region is NOT AWS Global, filter out any global quotas.
    final var clientBuilder = clientCreator.apply(ServiceQuotasClient.builder());
    if (region.isGlobalRegion()) {
      clientBuilder.region(Region.US_EAST_1);
    }

    try (final var client = clientBuilder.build()) {

      client.listServicesPaginator(ListServicesRequest.builder().build()).forEach(svcResponse ->
        svcResponse.services().forEach(svc -> {
            client.listServiceQuotasPaginator(ListServiceQuotasRequest.builder().serviceCode(svc.serviceCode()).build()).quotas()
              .stream()
              .filter(quota -> region.isGlobalRegion() == quota.globalQuota())  // If global only show global quotas, if not global match only non-global quotas.
              .filter(quota -> config.getServiceQuotas().isEmpty() || config.getServiceQuotas().contains(svc.serviceCode()))
              .forEach(quota -> {

                final var tags = client.listTagsForResource(ListTagsForResourceRequest.builder().resourceARN(quota.quotaArn()).build());
                var builder = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, quota.quotaArn())
                  .withResourceName(quota.quotaName())
                  .withResourceId(quota.quotaArn())
                  .withResourceType(RESOURCE_TYPE)
                  .withConfiguration(mapper.valueToTree(quota.toBuilder()))
                  .withAccountId(account)
                  .withTags(getConvertedTags(tags.tags(), mapper))
                  .withAwsRegion(region.toString());

                if (quota.usageMetric() != null) {
                  try(final var cwClient = clientCreator.apply(CloudWatchClient.builder()).build()) {
                    final var metric = quota.usageMetric();
                    final var md = metric.metricDimensions();
                    final var dimensions = md.keySet().stream().map(k -> Dimension.builder().name(k).value(md.get(k)).build()).collect(Collectors.toList());

                    final var metrics = cwClient.getMetricStatistics(GetMetricStatisticsRequest.builder()
                        .startTime(Instant.now().minus(24, ChronoUnit.HOURS))
                        .endTime(Instant.now())
                        .period(86400)
                        .statistics(Statistic.MAXIMUM)
                        .namespace(metric.metricNamespace())
                        .metricName(metric.metricName())
                        .dimensions(dimensions)
                      .build());
                    builder.withSupplementaryConfiguration(mapper.valueToTree(Map.of("reported", metrics.toBuilder())));
                  }
                } else {
                  //  Look for metrics what we have implemented custom counters for
                  var opt = COUNTERS.stream()
                    .filter(counter -> counter.quotaCode().equals(quota.quotaCode()))
                    .findFirst();
                  if (opt.isPresent()) {
                    final var count = opt.get().count(clientCreator);
                    builder.withSupplementaryConfiguration(mapper.valueToTree(Map.of("counted", count)));
                  }
                }

                emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":quota"), builder.build().toJsonNode()));
              });
          }
        ));
    }
  }

  private JsonNode getConvertedTags(List<Tag> tags, ObjectMapper mapper) {
    return mapper.convertValue(tags.stream().collect(
      Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
  }
}
