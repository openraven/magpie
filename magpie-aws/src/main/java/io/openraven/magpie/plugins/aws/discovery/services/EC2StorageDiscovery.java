/*
 * Copyright 2022 Open Raven Inc
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
import io.openraven.magpie.data.aws.ec2storage.EC2Snapshot;
import io.openraven.magpie.data.aws.ec2storage.EC2Volume;
import io.openraven.magpie.plugins.aws.discovery.AWSDiscoveryPlugin;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeSnapshotsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class EC2StorageDiscovery implements AWSDiscovery {

  private static final String SERVICE = "ec2storage";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {
    try (final var client = clientCreator.apply(Ec2Client.builder()).build()) {
      discoverSnapshots(mapper, session, client, region, emitter, account, logger);
//      discoverVolumes(mapper, session, client, region, emitter, account, logger);
    }
  }

  private void discoverSnapshots(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account, Logger logger) {
    final String RESOURCE_TYPE = EC2Snapshot.RESOURCE_TYPE;

    try {

      String nextToken = null;
      do {
        var resp = client.describeSnapshots(DescribeSnapshotsRequest.builder().ownerIds(account).nextToken(nextToken).build());
        nextToken = resp.nextToken();

        resp.snapshots()
          .forEach(snapshot -> {
            String arn = format("arn:aws:ec2:%s:%s:snapshot/%s", region, account, snapshot.snapshotId());
            logger.debug("Discovering {}", arn);
            var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, arn)
              .withResourceName(snapshot.snapshotId())
              .withResourceId(snapshot.snapshotId())
              .withResourceType(RESOURCE_TYPE)
              .withConfiguration(mapper.valueToTree(snapshot.toBuilder()))
              .withAccountId(account)
              .withAwsRegion(region.toString())
              .withTags(getConvertedTags(snapshot.tags(), mapper))
              .build();

            emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":Volume"), data.toJsonNode()));
          });
      } while (nextToken != null);

    } catch (Ec2Exception ex) {
      logger.info("Error discovering snapshots", ex);
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverVolumes(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account, Logger logger) {
    final String RESOURCE_TYPE = EC2Volume.RESOURCE_TYPE;
    try {

      String nextToken = null;
      do {
        var response = client.describeVolumes(DescribeVolumesRequest.builder().nextToken(nextToken).build());
        nextToken = response.nextToken();

        response.volumes()
          .forEach(volume -> {
            String arn = format("arn:aws:ec2:%s:%s:volume/%s", region, account, volume.volumeId());
            logger.debug("Discovering {}", arn);
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
      } while (nextToken != null);

    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private JsonNode getConvertedTags(List<Tag> tags, ObjectMapper mapper) {
    return mapper.convertValue(tags.stream().collect(
      Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
  }

  @Override
  public List<Region> getSupportedRegions() {
    return Ec2Client.serviceMetadata().regions();
  }
}
