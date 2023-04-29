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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieAwsResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.aws.quotas.ServiceQuota;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.servicequotas.ServiceQuotasClient;
import software.amazon.awssdk.services.servicequotas.model.ListServiceQuotasRequest;
import software.amazon.awssdk.services.servicequotas.model.ListServicesRequest;

import java.util.LinkedList;
import java.util.List;

public class ServiceQuotaDiscovery implements AWSDiscovery {
  private static final String SERVICE = "servicequotas";

//  // This is required due to the way S3 bucket data is implemented in the AWS SDK.  Finding the region for n-buckets
//  // requires n+1 API calls, and you can't filter bucket lists by region.  Using this cache we perform this operation once
//  // per session and cache it for a fixed number of minutes.
//  private static final Cache<String, List<software.amazon.awssdk.services.servicequotas.model.ServiceQuota>> QUOTA_CACHE = CacheBuilder.newBuilder()
//    .expireAfterAccess(Duration.ofMinutes(30))
//    .build();

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    var regions = new LinkedList<>(ServiceQuotasClient.serviceMetadata().regions());
    regions.add(Region.AWS_GLOBAL);
    return  regions;
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {
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
              .forEach(quota -> {
                var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, quota.quotaArn())
                  .withResourceName(quota.quotaName())
                  .withResourceId(quota.quotaArn())
                  .withResourceType(RESOURCE_TYPE)
                  .withConfiguration(mapper.valueToTree(quota.toBuilder()))
                  .withAccountId(account)
                  .withAwsRegion(region.toString())
                  .build();

                // TODO: Grab metrics from CW for each quota/region.

                emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":quota"), data.toJsonNode()));
            });

          }
        ));
    }
  }

//  private List<software.amazon.awssdk.services.servicequotas.model.ServiceQuota> getQuotas(String cacheKey, ServiceQuotasClient client, Logger logger) {
//    try {
//      //
//      // This method is executed whenever the bucket cache does not contain entries for a given cacheKey.  This ensures
//      // that we only make this expensive computation the lesser of once per scan or once per timeout period (defined above).
//      //
//      var list = new LinkedList<software.amazon.awssdk.services.servicequotas.model.ServiceQuota>();
//      return QUOTA_CACHE.get(cacheKey, () -> {
//        logger.debug("No cache found for {}, creating one now.", cacheKey);
//        client.listServicesPaginator(ListServicesRequest.builder().build()).services().forEach(svc -> list.addAll(client.listServiceQuotasPaginator(ListServiceQuotasRequest.builder().serviceCode(svc.serviceCode()).build()).quotas().stream()
////          .filter(q -> q.usageMetric() != null)
////            .filter(software.amazon.awssdk.services.servicequotas.model.ServiceQuota::globalQuota)
//          .collect(Collectors.toList())));
//        return list;
//      });
//
//    } catch (ExecutionException ex) {
//      throw new RuntimeException("Quota enumeration failed", ex);
//    }
//  }
}
