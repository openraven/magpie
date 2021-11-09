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
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.emr.EmrClient;
import software.amazon.awssdk.services.emr.model.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class EMRDiscovery implements AWSDiscovery {

  private static final String SERVICE = "emr";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return EmrClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    final var client = AWSUtils.configure(EmrClient.builder(), region);
    final String RESOURCE_TYPE = "AWS::EMR::Cluster";

    try {
      client.listClustersPaginator().clusters().stream().forEach(cluster -> {
        var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, cluster.clusterArn())
          .withResourceName(cluster.name())
          .withResourceId(cluster.id())
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(cluster.toBuilder()))
          .withCreatedIso(cluster.status().timeline().creationDateTime())
          .withAccountId(account)
          .withAwsRegion(region.toString())
          .build();

        discoverSteps(client, cluster, data);
        discoverInstances(client, cluster, data);
        discoverInstanceFleets(client, cluster, data);
        discoverInstanceGroups(client, cluster, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":cluster"), data.toJsonNode()));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverSteps(EmrClient client, ClusterSummary resource, MagpieAwsResource data) {
    final String keyname = "steps";

    getAwsResponse(
      () -> client.listStepsPaginator(ListStepsRequest.builder().clusterId(resource.id()).build()).steps()
        .stream()
        .map(StepSummary::toBuilder)
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverInstances(EmrClient client, ClusterSummary resource, MagpieAwsResource data) {
    final String keyname = "instances";

    getAwsResponse(
      () -> client.listInstancesPaginator(ListInstancesRequest.builder().clusterId(resource.id()).build())
        .stream()
        .map(ListInstancesResponse::toBuilder)
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverInstanceFleets(EmrClient client, ClusterSummary resource, MagpieAwsResource data) {
    final String keyname = "instancesFleet";

    getAwsResponse(
      () -> client.listInstanceFleetsPaginator(ListInstanceFleetsRequest.builder().clusterId(resource.id()).build()).instanceFleets()
        .stream()
        .map(InstanceFleet::toBuilder)
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverInstanceGroups(EmrClient client, ClusterSummary resource, MagpieAwsResource data) {
    final String keyname = "instancesGroups";

    getAwsResponse(
      () -> client.listInstanceGroups(ListInstanceGroupsRequest.builder().clusterId(resource.id()).build()).instanceGroups()
        .stream()
        .map(InstanceGroup::toBuilder)
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }
}
