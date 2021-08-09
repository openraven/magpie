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
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.Conversions;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.javatuples.Pair;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class RDSDiscovery implements AWSDiscovery {

  private static final String SERVICE = "rds";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return RdsClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    final var client = AWSUtils.configure(RdsClient.builder(), region);

    discoverDbSnapshot(mapper, session, region, emitter, account, client);
    discoverDbInstances(mapper, session, region, emitter, logger, account, client);
  }

  private void discoverDbSnapshot(ObjectMapper mapper, Session session, Region region, Emitter emitter, String account, RdsClient client) {
    final String RESOURCE_TYPE = "AWS::RDS::DBSnapshot";

    try {
      client.describeDBSnapshots(DescribeDbSnapshotsRequest.builder().includeShared(true).includePublic(false).build()).dbSnapshots()
        .forEach(snapshot -> {
          var data = new MagpieResource.MagpieResourceBuilder(mapper, snapshot.dbSnapshotArn())
            .withResourceName(snapshot.dbSnapshotIdentifier())
            .withResourceId(snapshot.dbSnapshotArn())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(snapshot.toBuilder()))
            .withCreatedIso(snapshot.instanceCreateTime())
            .withAccountId(account)
            .withRegion(region.toString())
            .build();

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":dbSnapshot"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverDbInstances(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, RdsClient client) {
    final String RESOURCE_TYPE = "AWS::RDS::DBInstance";

    try {
      client.describeDBInstancesPaginator().dbInstances().stream()
        .forEach(db -> {
          var data = new MagpieResource.MagpieResourceBuilder(mapper, db.dbInstanceArn())
            .withResourceName(db.dbInstanceIdentifier())
            .withResourceId(db.dbInstanceArn())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(db.toBuilder()))
            .withCreatedIso(db.instanceCreateTime())
            .withAccountId(account)
            .withRegion(region.toString())
            .build();

          if (db.instanceCreateTime() == null) {
            logger.warn("DBInstance has NULL CreateTime: dbInstanceArn=\"{}\"", db.dbInstanceArn());
          }

          discoverTags(client, db, data, mapper);
          discoverInstanceDbClusters(client, db, data);
          discoverInstanceDbSnapshots(client, db, data);
          discoverInstanceSize(db, data, logger);

          discoverBackupJobs(db.dbInstanceArn(), region, data);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":dbInstance"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverTags(RdsClient client, DBInstance resource, MagpieResource data, ObjectMapper mapper) {
    getAwsResponse(
      () -> client.listTagsForResource(ListTagsForResourceRequest.builder().resourceName(resource.dbInstanceArn()).build()),
      (resp) -> {
        JsonNode tagsNode = mapper.convertValue(resp.tagList().stream()
          .collect(Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
        AWSUtils.update(data.tags, tagsNode);
      },
      (noresp) -> AWSUtils.update(data.tags, noresp)
    );
  }

  private void discoverInstanceDbClusters(RdsClient client, DBInstance resource, MagpieResource data) {
    final String keyname = "dbClusters";
    getAwsResponse(
      () -> client.describeDBClusters(DescribeDbClustersRequest.builder().dbClusterIdentifier(resource.dbClusterIdentifier()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverInstanceDbSnapshots(RdsClient client, DBInstance resource, MagpieResource data) {
    final String keyname = "dbSnapshot";
    getAwsResponse(
      () -> client.describeDBSnapshots(DescribeDbSnapshotsRequest.builder()
        .dbInstanceIdentifier(resource.dbInstanceIdentifier())
        .includePublic(false)
        .includeShared(true)
        .build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverInstanceSize(DBInstance resource, MagpieResource data, Logger logger) {
    // get the DB engine and call the relevant function (as although RDS uses same client, the metrics available are different)
    String engine = resource.engine();
    if (engine != null) {
      if ("docdb".equalsIgnoreCase(engine)) {
        // although DocDB uses RDS client, it's metrics are subtly different, so get metrics via setDocDBSize
        setDocDBSize(resource, data, logger);
      } else if (engine.startsWith("aurora")) {
        setAuroraDBSize(resource, data, logger);
      } else {
        setRDSSize(resource, data, logger);
      }
    } else {
      logger.warn("{} RDS instance is missing engine property", resource.dbInstanceIdentifier());
    }
  }

  private void setRDSSize(DBInstance resource, MagpieResource data, Logger logger) {
    try {
      List<Dimension> dimensions = new ArrayList<>();
      dimensions.add(Dimension.builder().name("DBInstanceIdentifier").value(resource.dbInstanceIdentifier()).build());
      Pair<Long, GetMetricStatisticsResponse> freeStorageSpace =
        AWSUtils.getCloudwatchMetricMinimum(data.region, "AWS/RDS", "FreeStorageSpace", dimensions);

      if (freeStorageSpace.getValue0() != null) {
        logger.warn("{} RDS instance is missing engine property", resource.dbInstanceIdentifier());
        AWSUtils.update(data.supplementaryConfiguration, Map.of("size", Map.of("FreeStorageSpace", freeStorageSpace.getValue0())));

        // pull the relevant node(s) from the payload object. See https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/rds.html
        long freeStorageCapacity = freeStorageSpace.getValue0();
        long storageCapacity = resource.allocatedStorage();

        data.sizeInBytes = Conversions.GibToBytes(storageCapacity) - freeStorageCapacity;
        data.maxSizeInBytes = Conversions.GibToBytes(storageCapacity);
      } else {
        logger.warn("{} RDS instance is missing size metrics", resource.dbInstanceIdentifier());
      }
    } catch (Exception se) {
      logger.warn("{} RDS instance is missing size metrics, with error {}", resource.dbInstanceIdentifier(), se.getMessage());
    }
  }

  private void setDocDBSize(DBInstance resource, MagpieResource data, Logger logger) {
    try {
      List<Dimension> dimensions = new ArrayList<>();
      dimensions.add(Dimension.builder().name("DBClusterIdentifier").value(resource.dbInstanceIdentifier()).build());
      Pair<Long, GetMetricStatisticsResponse> volumeBytesUsed =
        AWSUtils.getCloudwatchMetricMaximum(data.region, "AWS/DocDB", "VolumeBytesUsed", dimensions);

      if (volumeBytesUsed.getValue0() != null) {
        AWSUtils.update(data.supplementaryConfiguration, Map.of("size", Map.of("VolumeBytesUsed", volumeBytesUsed.getValue0())));

        data.sizeInBytes = volumeBytesUsed.getValue0();
        data.maxSizeInBytes = Conversions.GibToBytes(resource.allocatedStorage());
      }
    } catch (Exception se) {
      logger.warn("{} RDS instance is missing size metrics, with error {}", resource.dbInstanceArn(), se.getMessage());
    }
  }

  private void setAuroraDBSize(DBInstance resource, MagpieResource data, Logger logger) {
    try {
      List<Dimension> dimensions = new ArrayList<>();
      dimensions.add(Dimension.builder().name("DBClusterIdentifier").value(resource.dbInstanceIdentifier()).build());
      Pair<Long, GetMetricStatisticsResponse> volumeBytesUsed =
        AWSUtils.getCloudwatchMetricMaximum(data.region, "AWS/RDS", "VolumeBytesUsed", dimensions);

      if (volumeBytesUsed.getValue0() != null) {
        AWSUtils.update(data.supplementaryConfiguration, Map.of("size", Map.of("VolumeBytesUsed", volumeBytesUsed.getValue0())));

        data.sizeInBytes = volumeBytesUsed.getValue0();
        data.maxSizeInBytes = Conversions.GibToBytes(resource.allocatedStorage());

      }
    } catch (Exception se) {
      logger.warn("{} RDS instance is missing size metrics, with error {}", resource.dbInstanceArn(), se.getMessage());
    }
  }
}
