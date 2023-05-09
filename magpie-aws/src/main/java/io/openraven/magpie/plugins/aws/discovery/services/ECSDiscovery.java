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
import io.openraven.magpie.api.MagpieAwsResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.aws.ecs.EcsCluster;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.Cluster;
import software.amazon.awssdk.services.ecs.model.DescribeClustersRequest;
import software.amazon.awssdk.services.ecs.model.DescribeServicesRequest;
import software.amazon.awssdk.services.ecs.model.DescribeServicesResponse;
import software.amazon.awssdk.services.ecs.model.DescribeTasksRequest;
import software.amazon.awssdk.services.ecs.model.DescribeTasksResponse;
import software.amazon.awssdk.services.ecs.model.ListAttributesRequest;
import software.amazon.awssdk.services.ecs.model.ListServicesRequest;
import software.amazon.awssdk.services.ecs.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.ecs.model.ListTasksRequest;
import software.amazon.awssdk.services.ecs.model.Tag;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class ECSDiscovery implements AWSDiscovery {

  private static final String SERVICE = "ecs";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return EcsClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {
    final String RESOURCE_TYPE = EcsCluster.RESOURCE_TYPE;

    try (final var client = clientCreator.apply(EcsClient.builder()).build()) {
      listDescribedClusters(client).forEach(cluster -> {
        var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, cluster.clusterArn())
          .withResourceName(cluster.clusterName())
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(cluster.toBuilder()))
          .withAccountId(account)
          .withAwsRegion(region.toString())
          .build();

        discoverAttributes(client, cluster, data);
        discoverServices(client, cluster, data);
        discoverTasks(client, cluster, data);
        discoverTags(client, cluster, data, mapper);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":cluster"), data.toJsonNode()));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private List<Cluster> listDescribedClusters(EcsClient client) {

    final var list = new LinkedList<Cluster>();
    String nextToken = null;
    do {
      final var resp = client.listClusters();
      nextToken = resp.nextToken();
      list.addAll(client.describeClusters(DescribeClustersRequest.builder().clusters(resp.clusterArns()).build()).clusters());
    } while (nextToken != null);

    return list;
  }

  private void discoverTags(EcsClient client, Cluster resource, MagpieAwsResource data, ObjectMapper mapper) {
    getAwsResponse(
      () -> client.listTagsForResource(ListTagsForResourceRequest.builder().resourceArn(resource.clusterArn()).build()),
      (resp) -> {
        JsonNode tagsNode = mapper.convertValue(resp.tags().stream()
          .collect(Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
        AWSUtils.update(data.tags, tagsNode);
      },
      (noresp) -> AWSUtils.update(data.tags, noresp)
    );
  }

  private void discoverAttributes(EcsClient client, Cluster resource, MagpieAwsResource data) {
    final String keyname = "attributes";
    getAwsResponse(
      () -> client.listAttributes(ListAttributesRequest.builder().targetType("container-instance").cluster(resource.clusterArn()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverServices(EcsClient client, Cluster resource, MagpieAwsResource data) {
    final String keyname = "services";
    getAwsResponse(
      () -> listDescribedServices(client, resource),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private DescribeServicesResponse listDescribedServices(EcsClient client, Cluster resource) {
    var serviceArns =
      client.listServicesPaginator(ListServicesRequest.builder().cluster(resource.clusterArn()).build()).serviceArns()
        .stream()
        .collect(Collectors.toList());

    return client.describeServices(DescribeServicesRequest.builder().cluster(resource.clusterArn()).services(serviceArns).build());
  }

  private void discoverTasks(EcsClient client, Cluster resource, MagpieAwsResource data) {
    final String keyname = "tasks";
    getAwsResponse(
      () -> listDescribedTasks(client, resource),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private DescribeTasksResponse listDescribedTasks(EcsClient client, Cluster resource) {
    var instanceArns = client.listTasksPaginator(ListTasksRequest.builder().cluster(resource.clusterArn()).build()).taskArns()
      .stream()
      .collect(Collectors.toList());

    return client.describeTasks(DescribeTasksRequest.builder().cluster(resource.clusterArn()).tasks(instanceArns).build());
  }
}
