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
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;
import static java.util.Arrays.asList;

public class ECSDiscovery implements AWSDiscovery {

  private static final String SERVICE = "ecs";

  private final List<LocalDiscovery> discoveryMethods = asList(
    this::discoverAttributes,
    this::discoverServices,
    this::discoverTasks,
    this::discoverTags
  );

  @FunctionalInterface
  interface LocalDiscovery {
    void discover(EcsClient client, Cluster resource, ObjectNode data, Logger logger, ObjectMapper mapper);
  }

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return EcsClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger) {
    final var client = EcsClient.builder().region(region).build();

    getAwsResponse(
      () -> listDescribedClusters(client),
      (resp) -> resp.forEach(cluster -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", cluster.toBuilder());
        data.put("region", region.toString());

        for (var dm : discoveryMethods)
          dm.discover(client, cluster, data, logger, mapper);

        emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":cluster"), data));
      }),
      (noresp) -> logger.error("Failed to get clusters in {}", region)
    );
  }

  private List<Cluster> listDescribedClusters(EcsClient client) {
    var clusterArns = client.listClustersPaginator().clusterArns()
      .stream()
      .collect(Collectors.toList());

    return client.describeClusters(DescribeClustersRequest.builder().clusters(clusterArns).build()).clusters();
  }

  private void discoverTags(EcsClient client, Cluster resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    logger.trace("Getting tags for {}", resource.clusterArn());
    var obj = data.putObject("tags");
    getAwsResponse(
      () -> client.listTagsForResource(ListTagsForResourceRequest.builder().resourceArn(resource.clusterArn()).build()),
      (resp) -> {
        JsonNode tagsNode = mapper.convertValue(resp.tags().stream()
          .collect(Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
        AWSUtils.update(obj, tagsNode);
      },
      (noresp) -> AWSUtils.update(obj, noresp)
    );
  }

  private void discoverAttributes(EcsClient client, Cluster resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    logger.trace("Getting attributes for {}", resource.clusterArn());
    final String keyname = "attributes";
    getAwsResponse(
      () -> client.listAttributes(ListAttributesRequest.builder().targetType("container-instance").cluster(resource.clusterArn()).build()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private void discoverServices(EcsClient client, Cluster resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    logger.trace("Getting services for {}", resource.clusterArn());
    final String keyname = "services";
    getAwsResponse(
      () -> listDescribedServices(client, resource),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private DescribeServicesResponse listDescribedServices(EcsClient client, Cluster resource) {
    var serviceArns =
      client.listServicesPaginator(ListServicesRequest.builder().cluster(resource.clusterArn()).build()).serviceArns()
        .stream()
        .collect(Collectors.toList());

    return client.describeServices(DescribeServicesRequest.builder().cluster(resource.clusterArn()).services(serviceArns).build());
  }

  private void discoverTasks(EcsClient client, Cluster resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    logger.trace("Getting tasks for {}", resource.clusterArn());
    final String keyname = "tasks";
    getAwsResponse(
      () -> listDescribedTasks(client, resource),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private DescribeTasksResponse listDescribedTasks(EcsClient client, Cluster resource) {
    var instanceArns = client.listTasksPaginator(ListTasksRequest.builder().cluster(resource.clusterArn()).build()).taskArns()
      .stream()
      .collect(Collectors.toList());

    return client.describeTasks(DescribeTasksRequest.builder().cluster(resource.clusterArn()).tasks(instanceArns).build());
  }
}
