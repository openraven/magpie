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
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSResource;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.DistributionSummary;
import software.amazon.awssdk.services.cloudfront.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.cloudfront.model.Tag;

import java.util.List;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class CloudFrontDiscovery implements AWSDiscovery {

  private static final String SERVICE = "cloudFront";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return CloudFrontClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    final var client = CloudFrontClient.builder().region(region).build();

    getAwsResponse(
      () -> client.listDistributions().distributionList().items(),
      (resp) -> resp.forEach(distribution -> {
        var data = new AWSResource(distribution.toBuilder(), region.toString(), account, mapper);
        data.arn = distribution.arn();
        data.resourceId = distribution.id();
        data.resourceName = distribution.domainName();
        data.resourceType = "AWS::CloudFront::Distribution";
        data.updatedIso = distribution.lastModifiedTime().toString();

        discoverTags(client, distribution, data, mapper);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":distribution"), data.toJsonNode(mapper)));
      }),
      (noresp) -> logger.error("Failed to get distributions in {}", region)
    );
  }

  private void discoverTags(CloudFrontClient client, DistributionSummary resource, AWSResource data, ObjectMapper mapper) {
    getAwsResponse(
      () -> client.listTagsForResource(ListTagsForResourceRequest.builder().resource(resource.arn()).build()),
      (resp) -> {
        JsonNode tagsNode = mapper.convertValue(resp.tags().items().stream()
          .collect(Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
        AWSUtils.update(data.tags, tagsNode);
      },
      (noresp) -> AWSUtils.update(data.tags, noresp)
    );
  }
}
