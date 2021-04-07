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
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;
import software.amazon.awssdk.services.cloudtrail.model.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class CloudTrailDiscovery implements AWSDiscovery {

  private static final String SERVICE = "cloudTrail";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return CloudTrailClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger) {
    final var client = CloudTrailClient.builder().region(region).build();

    getAwsResponse(
      () -> client.listTrailsPaginator(ListTrailsRequest.builder().build()).trails(),
      (resp) -> resp.forEach(trail -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", trail.toBuilder());
        data.put("region", region.toString());

        discoverEventSelectors(client, trail, data);
        discoverInsightSelectors(client, trail, data);
        discoverTrailDetails(client, trail, data);
        discoverTrailStatus(client, trail, data);
        discoverTags(client, trail, data, mapper);

        emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":trail"), data));
      }),
      (noresp) -> logger.error("Failed to get trails in {}", region)
    );
  }

  private void discoverEventSelectors(CloudTrailClient client, TrailInfo resource, ObjectNode data) {
    final String keyname = "eventSelectors";

    getAwsResponse(
      () -> client.getEventSelectors(GetEventSelectorsRequest.builder().trailName(resource.trailARN()).build()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private void discoverInsightSelectors(CloudTrailClient client, TrailInfo resource, ObjectNode data) {
    final String keyname = "insightSelectors";

    getAwsResponse(
      () -> client.getInsightSelectors(GetInsightSelectorsRequest.builder().trailName(resource.trailARN()).build()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private void discoverTrailDetails(CloudTrailClient client, TrailInfo resource, ObjectNode data) {
    final String keyname = "trailDetails";

    getAwsResponse(
      () -> client.getTrail(GetTrailRequest.builder().name(resource.trailARN()).build()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private void discoverTrailStatus(CloudTrailClient client, TrailInfo resource, ObjectNode data) {
    final String keyname = "status";

    getAwsResponse(
      () -> client.getTrailStatus(GetTrailStatusRequest.builder().name(resource.trailARN()).build()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private void discoverTags(CloudTrailClient client, TrailInfo resource, ObjectNode data, ObjectMapper mapper) {
    var obj = data.putObject("tags");

    getAwsResponse(
      () -> client.listTagsPaginator(ListTagsRequest.builder().resourceIdList(resource.trailARN()).build()).resourceTagList().stream().findFirst(),
      (resp) -> {
        if(resp.isPresent()) {
          JsonNode tagsNode = mapper.convertValue(resp.get().tagsList().stream()
            .collect(Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
          AWSUtils.update(obj, tagsNode);
        }
      },
      (noresp) -> AWSUtils.update(obj, noresp)
    );
  }
}