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
import io.openraven.magpie.data.aws.ec2.*;
import io.openraven.magpie.data.aws.ec2storage.EC2Snapshot;
import io.openraven.magpie.data.aws.ec2storage.EC2Volume;
import io.openraven.magpie.plugins.aws.discovery.AWSDiscoveryPlugin;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
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
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {

    try (final var client = clientCreator.apply(Ec2Client.builder()).build()) {
      discoverEc2Instances(mapper, session, client, region, emitter, account, clientCreator, logger);
      discoverEIPs(mapper, session, client, region, emitter, account);
      discoverSecurityGroups(mapper, session, client, region, emitter, account);
      discoverVolumes(mapper, session, client, region, emitter, account);
      discoverSnapshots(mapper, session, client, region, emitter, account);
      discoverNetworkAcls(mapper, session, client, region, emitter, account);
      discoverNetworkInterfaces(mapper, session, client, region, emitter, account);
    }
  }

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return Ec2Client.serviceMetadata().regions();
  }

  private void discoverEc2Instances(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account, MagpieAWSClientCreator clientCreator, Logger logger) {

    final String RESOURCE_TYPE = Ec2Instance.RESOURCE_TYPE;
    try {
      client.describeInstancesPaginator()
        .forEach(describeInstancesResponse -> describeInstancesResponse.reservations()
          .forEach(reservation -> reservation.instances().forEach(instance -> {
            String arn = format("arn:aws:ec2:%s:%s:instance/%s", region, reservation.ownerId(), instance.instanceId());
            var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, arn)
              .withResourceName(instance.instanceId())
              .withResourceId(instance.instanceId())
              .withResourceType(RESOURCE_TYPE)
              .withConfiguration(mapper.valueToTree(instance.toBuilder()))
              .withCreatedIso(instance.launchTime())
              .withAccountId(account)
              .withAwsRegion(region.toString())
              .withTags(getConvertedTags(instance.tags(), mapper))
              .build();

            massageInstanceTypeAndPublicIp(data, instance, mapper, region, RESOURCE_TYPE);
            discoverBackupJobs(arn, region, data, clientCreator);
            emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService()), data.toJsonNode()));
          })));
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  public void massageInstanceTypeAndPublicIp(MagpieAwsResource data,
                                             Instance instance,
                                             ObjectMapper mapper,
                                             Region region,
                                             String resourceType) {
    try {
      var instanceForUpdate = mapper.readerForUpdating(data.configuration);

      data.configuration = instanceForUpdate.readValue(mapper.convertValue(
        Map.of("instanceType", instance.instanceTypeAsString()), JsonNode.class));

      if (!StringUtils.isEmpty(instance.publicIpAddress())) {
        data.configuration = instanceForUpdate.readValue(mapper.convertValue(
          Map.of("publicIp", instance.publicIpAddress()), JsonNode.class));
      }

    } catch (IOException ex) {
      DiscoveryExceptions.onDiscoveryException(resourceType, null, region, ex);
    }
  }

  private void discoverEIPs(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = Ec2ElasticIpAddress.RESOURCE_TYPE;

    try {
      client.describeAddresses().addresses().forEach(eip -> {
        String arn = format("arn:aws:ec2:%s:%s:eip-allocation/%s", region, account, eip.allocationId());
        var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, arn)
          .withResourceName(eip.publicIp())
          .withResourceId(eip.allocationId())
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(eip.toBuilder()))
          .withAccountId(account)
          .withAwsRegion(region.toString())
          .withTags(getConvertedTags(eip.tags(), mapper))
          .build();

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":eip"), data.toJsonNode()));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }

  }

  private void discoverSecurityGroups(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = EC2SecurityGroup.RESOURCE_TYPE;

    try {
      client.describeSecurityGroupsPaginator().stream()
        .flatMap(r -> r.securityGroups().stream())
        .forEach(securityGroup -> {
          String arn = format("arn:aws:ec2:%s:%s:security-group/%s", region, account, securityGroup.groupId());
          var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, arn)
            .withResourceName(securityGroup.groupName())
            .withResourceId(securityGroup.groupId())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(securityGroup.toBuilder()))
            .withAccountId(account)
            .withAwsRegion(region.toString())
            .withTags(getConvertedTags(securityGroup.tags(), mapper))
            .build();

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":securityGroup"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }

  }

  private void discoverVolumes(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = EC2Volume.RESOURCE_TYPE;
    try {
      client.describeVolumesPaginator().stream()
        .flatMap(r -> r.volumes().stream())
        .forEach(volume -> {
          String arn = format("arn:aws:ec2:%s:%s:volume/%s", region, account, volume.volumeId());
          var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, arn)
            .withResourceName(volume.volumeId())
            .withResourceId(volume.volumeId())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(volume.toBuilder()))
            .withAccountId(account)
            .withAwsRegion(region.toString())
            .withTags(getConvertedTags(volume.tags(), mapper))
            .build();

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":Volume"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverSnapshots(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = EC2Snapshot.RESOURCE_TYPE;

    try {
      client.describeSnapshotsPaginator(DescribeSnapshotsRequest.builder().ownerIds(account).build()).snapshots().stream()
        .forEach(snapshot -> {
          String arn = format("arn:aws:ec2:%s:%s:snapshot/%s", region, account, snapshot.snapshotId());
          var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, arn)
            .withResourceName(snapshot.snapshotId())
            .withResourceId(snapshot.snapshotId())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(snapshot.toBuilder()))
            .withAccountId(account)
            .withAwsRegion(region.toString())
            .withTags(getConvertedTags(snapshot.tags(), mapper))
            .build();

          discoverSnapshotVolumes(client, data, snapshot);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":Volume"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverSnapshotVolumes(Ec2Client client, MagpieAwsResource data, Snapshot snapshot) {
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

  private void discoverNetworkAcls(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = Ec2NetworkAcl.RESOURCE_TYPE;

    try {
      client.describeNetworkAclsPaginator(DescribeNetworkAclsRequest.builder().build()).networkAcls().stream()
        .forEach(acl -> {
          String arn = format("arn:aws:ec2:%s:%s:network-acl/%s", region, account, acl.networkAclId());
          var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, arn)
            .withResourceName(acl.networkAclId())
            .withResourceId(acl.networkAclId())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(acl.toBuilder()))
            .withAccountId(account)
            .withAwsRegion(region.toString())
            .withTags(getConvertedTags(acl.tags(), mapper))
            .build();

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":NetworkAcl"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverNetworkInterfaces(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = Ec2NetworkInterface.RESOURCE_TYPE;

    try {
      client.describeNetworkInterfacesPaginator(DescribeNetworkInterfacesRequest.builder().build()).networkInterfaces().stream()
        .forEach(networkInterface -> {
          String arn = format("arn:aws:ec2:%s:%s:network-interface/%s", region, account, networkInterface.networkInterfaceId());
          var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, arn)
            .withResourceName(networkInterface.networkInterfaceId())
            .withResourceId(networkInterface.networkInterfaceId())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(networkInterface.toBuilder()))
            .withAccountId(account)
            .withAwsRegion(region.toString())
            .withTags(getConvertedTags(networkInterface.tagSet(), mapper))
            .build();

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":NetworkAcl"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private JsonNode getConvertedTags(List<Tag> tags, ObjectMapper mapper) {
    return mapper.convertValue(tags.stream().collect(
      Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
  }
}
