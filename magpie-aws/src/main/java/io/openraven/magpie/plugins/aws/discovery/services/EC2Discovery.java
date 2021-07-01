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
import io.openraven.magpie.api.MagpieResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSDiscoveryPlugin;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;
import static java.lang.String.format;

public class EC2Discovery implements AWSDiscovery {

  private static final String SERVICE = "ec2";

  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    final var client = AWSUtils.configure(Ec2Client.builder(), region);

    discoverEc2Instances(mapper, session, client, region, emitter, account);
    discoverEIPs(mapper, session, client, region, emitter, account);
    discoverSecurityGroups(mapper, session, client, region, emitter, account);
    discoverVolumes(mapper, session, client, region, emitter, account);
    discoverSnapshots(mapper, session, client, region, emitter, account);
  }

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return Ec2Client.serviceMetadata().regions();
  }

  private void discoverEc2Instances(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::EC2::Instance";
    try {
      client.describeInstancesPaginator()
        .forEach(describeInstancesResponse -> describeInstancesResponse.reservations()
          .forEach(reservation -> reservation.instances().forEach(instance -> {
            String arn = format("arn:aws:ec2:%s:%s:instance/%s", region, reservation.ownerId(), instance.instanceId());
            var data = new MagpieResource.MagpieResourceBuilder(mapper, arn)
              .withResourceName(instance.instanceId())
              .withResourceId(instance.instanceId())
              .withResourceType(RESOURCE_TYPE)
              .withConfiguration(mapper.valueToTree(instance.toBuilder()))
              .withCreatedIso(instance.launchTime())
              .withAccountId(account)
              .withRegion(region.toString())
              .withTags(getConvertedTags(instance.tags(), mapper))
              .build();

            massageInstanceTypeAndPublicIp(data, instance, mapper);

            emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService()), data.toJsonNode()));
          })));
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  public void massageInstanceTypeAndPublicIp(MagpieResource data, Instance instance, ObjectMapper mapper) {
    try {
      var instanceForUpdate = mapper.readerForUpdating(data.configuration);

      data.configuration = instanceForUpdate.readValue(mapper.convertValue(
        Map.of("instanceType", instance.instanceTypeAsString()), JsonNode.class));

      if (!StringUtils.isEmpty(instance.publicIpAddress())) {
        data.configuration = instanceForUpdate.readValue(mapper.convertValue(
          Map.of("publicIp", instance.publicIpAddress()), JsonNode.class));
      }

    } catch (IOException ignored) {
    }
  }

  private void discoverEIPs(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::EC2::EIP";

    try {
      client.describeAddresses().addresses().forEach(eip -> {
        String arn = format("arn:aws:ec2:%s:%s:eip-allocation/%s", region, account, eip.allocationId());
        var data = new MagpieResource.MagpieResourceBuilder(mapper, arn)
          .withResourceName(eip.publicIp())
          .withResourceId(eip.allocationId())
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(eip.toBuilder()))
          .withAccountId(account)
          .withRegion(region.toString())
          .withTags(getConvertedTags(eip.tags(), mapper))
          .build();

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":eip"), data.toJsonNode()));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }

  }

  private void discoverSecurityGroups(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::EC2::SecurityGroup";

    try {
      client.describeSecurityGroupsPaginator().stream()
        .flatMap(r -> r.securityGroups().stream())
        .forEach(securityGroup -> {
          String arn = format("arn:aws:ec2:%s:%s:security-group/%s", region, account, securityGroup.groupId());
          var data = new MagpieResource.MagpieResourceBuilder(mapper, arn)
            .withResourceName(securityGroup.groupName())
            .withResourceId(securityGroup.groupId())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(securityGroup.toBuilder()))
            .withAccountId(account)
            .withRegion(region.toString())
            .withTags(getConvertedTags(securityGroup.tags(), mapper))
            .build();

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":securityGroup"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }

  }

  private void discoverVolumes(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::EC2::Volume";
    try {
      client.describeVolumesPaginator().stream()
        .flatMap(r -> r.volumes().stream())
        .forEach(volume -> {
          String arn = format("arn:aws:ec2:%s:%s:volume/%s", region, account, volume.volumeId());
          var data = new MagpieResource.MagpieResourceBuilder(mapper, arn)
            .withResourceName(volume.volumeId())
            .withResourceId(volume.volumeId())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(volume.toBuilder()))
            .withAccountId(account)
            .withRegion(region.toString())
            .withTags(getConvertedTags(volume.tags(), mapper))
            .build();

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":Volume"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverSnapshots(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::EC2::Snapshot";

    try {
      client.describeSnapshotsPaginator(DescribeSnapshotsRequest.builder().ownerIds(account).build()).snapshots().stream()
        .forEach(snapshot -> {
          String arn = format("arn:aws:ec2:%s:%s:snapshot/%s", region, account, snapshot.snapshotId());
          var data = new MagpieResource.MagpieResourceBuilder(mapper, arn)
            .withResourceName(snapshot.snapshotId())
            .withResourceId(snapshot.snapshotId())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(snapshot.toBuilder()))
            .withAccountId(account)
            .withRegion(region.toString())
            .withTags(getConvertedTags(snapshot.tags(), mapper))
            .build();

          discoverSnapshotVolumes(client, data, snapshot);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":Volume"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverSnapshotVolumes(Ec2Client client, MagpieResource data, Snapshot snapshot) {
    final String keyname = "volumes";
    var snapshotFilter = Filter.builder().name("snapshot-id").values(List.of(snapshot.snapshotId())).build();

    getAwsResponse(
      () -> client.describeVolumesPaginator(DescribeVolumesRequest.builder().filters(List.of(snapshotFilter)).build())
        .stream()
        .map(DescribeVolumesResponse::toBuilder)
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private JsonNode getConvertedTags(List<Tag> tags, ObjectMapper mapper) {
    return mapper.convertValue(tags.stream().collect(
      Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
  }
}
