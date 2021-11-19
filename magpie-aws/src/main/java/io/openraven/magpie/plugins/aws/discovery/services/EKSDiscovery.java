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
import io.openraven.magpie.api.MagpieResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eks.EksClient;
import software.amazon.awssdk.services.eks.model.Cluster;
import software.amazon.awssdk.services.eks.model.DescribeClusterRequest;
import software.amazon.awssdk.services.eks.model.DescribeFargateProfileRequest;
import software.amazon.awssdk.services.eks.model.DescribeNodegroupRequest;
import software.amazon.awssdk.services.eks.model.DescribeUpdateRequest;
import software.amazon.awssdk.services.eks.model.DescribeUpdateResponse;
import software.amazon.awssdk.services.eks.model.FargateProfile;
import software.amazon.awssdk.services.eks.model.ListFargateProfilesRequest;
import software.amazon.awssdk.services.eks.model.ListNodegroupsRequest;
import software.amazon.awssdk.services.eks.model.ListUpdatesRequest;
import software.amazon.awssdk.services.eks.model.Nodegroup;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class EKSDiscovery implements AWSDiscovery {

  private static final String SERVICE = "eks";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return EksClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {
    final String RESOURCE_TYPE = "AWS::EKS::Cluster";

    try (final var client = clientCreator.apply(EksClient.builder()).build()) {
      client.listClustersPaginator().clusters()
        .stream()
        .map(clusterName -> client.describeCluster(DescribeClusterRequest.builder().name(clusterName).build()))
        .forEach(cluster -> {
          var data = new MagpieResource.MagpieResourceBuilder(mapper, cluster.cluster().arn())
            .withResourceName(cluster.cluster().name())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(cluster.toBuilder()))
            .withAccountId(account)
            .withRegion(region.toString())
            .build();

          discoverFargateProfiles(client, cluster.cluster(), data);
          discoverNodegroups(client, cluster.cluster(), data);
          discoverUpdates(client, cluster.cluster(), data);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":cluster"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverFargateProfiles(EksClient client, Cluster cluster, MagpieResource data) {
    final String keyname = "fargateProfiles";

    getAwsResponse(
      () -> client.listFargateProfilesPaginator(ListFargateProfilesRequest.builder().clusterName(cluster.name()).build()).fargateProfileNames()
        .stream()
        .map(profileName -> client.describeFargateProfile(
          DescribeFargateProfileRequest.builder().clusterName(cluster.name()).fargateProfileName(profileName).build()).fargateProfile())
        .map(FargateProfile::toBuilder)
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverNodegroups(EksClient client, Cluster cluster, MagpieResource data) {
    final String keyname = "nodegroups";

    getAwsResponse(
      () -> client.listNodegroups(ListNodegroupsRequest.builder().clusterName(cluster.name()).build()).nodegroups()
        .stream()
        .map(nodegroupName -> client.describeNodegroup(
          DescribeNodegroupRequest.builder().clusterName(cluster.name()).nodegroupName(nodegroupName).build()).nodegroup())
        .map(Nodegroup::toBuilder)
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverUpdates(EksClient client, Cluster cluster, MagpieResource data) {
    final String keyname = "updates";

    getAwsResponse(
      () -> client.listUpdates(ListUpdatesRequest.builder().name(cluster.name()).build()).updateIds()
        .stream()
        .map(updateId -> client.describeUpdate(
          DescribeUpdateRequest.builder().name(cluster.name()).updateId(updateId).build()))
        .map(DescribeUpdateResponse::toBuilder)
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }
}
