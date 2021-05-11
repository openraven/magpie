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
import io.openraven.magpie.plugins.aws.discovery.AWSDiscoveryPlugin;
import io.openraven.magpie.plugins.aws.discovery.AWSResource;
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
    final var client = Ec2Client.builder().region(region).build();

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
            var data = new AWSResource(instance.toBuilder(), region.toString(), account, mapper);
            data.resourceName = instance.instanceId();
            data.resourceId = instance.instanceId();
            data.resourceType = RESOURCE_TYPE;
            data.arn = format("arn:aws:ec2:%s:%s:instance/%s", region, reservation.ownerId(), instance.instanceId());
            data.createdIso = instance.launchTime();
            data.tags = getConvertedTags(instance.tags(), mapper);

            massageInstanceTypeAndPublicIp(data, instance, mapper);

            emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService()), data.toJsonNode(mapper)));
          })));
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  public void massageInstanceTypeAndPublicIp(AWSResource data, Instance instance, ObjectMapper mapper) {
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
        var data = new AWSResource(eip.toBuilder(), region.toString(), account, mapper);
        data.arn = format("arn:aws:ec2:%s:%s:eip-allocation/%s", region, account, eip.allocationId());
        data.resourceId = eip.allocationId();
        data.resourceName = eip.publicIp();
        data.resourceType = RESOURCE_TYPE;
        data.tags = getConvertedTags(eip.tags(), mapper);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":eip"), data.toJsonNode(mapper)));
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
          var data = new AWSResource(securityGroup.toBuilder(), region.toString(), account, mapper);
          data.arn = format("arn:aws:ec2:%s:%s:security-group/%s", region, account, securityGroup.groupId());
          data.resourceId = securityGroup.groupId();
          data.resourceName = securityGroup.groupName();
          data.resourceType = RESOURCE_TYPE;

          data.tags = getConvertedTags(securityGroup.tags(), mapper);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":securityGroup"), data.toJsonNode(mapper)));
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
          var data = new AWSResource(volume.toBuilder(), region.toString(), account, mapper);
          data.arn = format("arn:aws:ec2:%s:%s:volume/%s", region, account, volume.volumeId());
          data.resourceId = volume.volumeId();
          data.resourceName = volume.volumeId();
          data.resourceType = RESOURCE_TYPE;
          data.tags = getConvertedTags(volume.tags(), mapper);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":Volume"), data.toJsonNode(mapper)));
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
          var data = new AWSResource(snapshot.toBuilder(), region.toString(), account, mapper);
          data.arn = format("arn:aws:ec2:%s:%s:snapshot/%s", region, account, snapshot.snapshotId());
          data.resourceId = snapshot.snapshotId();
          data.resourceName = snapshot.snapshotId();
          data.resourceType = RESOURCE_TYPE;
          data.tags = getConvertedTags(snapshot.tags(), mapper);

          discoverSnapshotVolumes(client, data, snapshot);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":Volume"), data.toJsonNode(mapper)));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverSnapshotVolumes(Ec2Client client, AWSResource data, Snapshot snapshot) {
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
