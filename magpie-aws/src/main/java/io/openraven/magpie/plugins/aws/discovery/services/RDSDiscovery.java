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
import io.openraven.magpie.plugins.aws.discovery.Conversions;
import org.javatuples.Pair;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbClustersRequest;
import software.amazon.awssdk.services.rds.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.rds.model.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;
import static java.util.Arrays.asList;

public class RDSDiscovery implements AWSDiscovery {

  private static final String SERVICE = "rds";

  private final List<LocalDiscovery> discoveryMethods = asList(
    this::discoverTags,
    this::discoverDbClusters,
    this::discoverSize
  );

  @FunctionalInterface
  interface LocalDiscovery {
    void discover(RdsClient client, DBInstance resource, ObjectNode data, Logger logger, ObjectMapper mapper);
  }

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return RdsClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger) {
    final var client = RdsClient.builder().region(region).build();

    try {
      client.describeDBInstancesPaginator().dbInstances().stream()
        .forEach(db -> {
          var data = mapper.createObjectNode();
          data.putPOJO("configuration", db.toBuilder());
          data.put("region", region.toString());

          if (db.instanceCreateTime() == null) {
            logger.warn("DBInstance has NULL CreateTime: dbInstanceArn=\"{}\"", db.dbInstanceArn());
          }

          for (var dm : discoveryMethods) {
            try {
              dm.discover(client, db, data, logger, mapper);
            } catch (SdkServiceException | SdkClientException ex) {
              logger.error("Failed to discover data for {}", db.dbInstanceArn(), ex);
            }
          }

          emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":dbInstance"), data));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      logger.error("Finished Rds bucket discovery in {}", region);
    }
  }

  private void discoverTags(RdsClient client, DBInstance resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    var obj = data.putObject("tags");
    getAwsResponse(
      () -> client.listTagsForResource(ListTagsForResourceRequest.builder().resourceName(resource.dbInstanceArn()).build()),
      (resp) -> {
        JsonNode tagsNode = mapper.convertValue(resp.tagList().stream()
          .collect(Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
        AWSUtils.update(obj, tagsNode);
      },
      (noresp) -> AWSUtils.update(obj, noresp)
    );
  }

  private void discoverDbClusters(RdsClient client, DBInstance resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    final String keyname = "DbClusters";
    getAwsResponse(
      () -> client.describeDBClusters(DescribeDbClustersRequest.builder().dbClusterIdentifier(resource.dbClusterIdentifier()).build()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private void discoverSize(RdsClient client, DBInstance resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
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

  private void setRDSSize(DBInstance resource, ObjectNode data, Logger logger) {
    try {
      List<Dimension> dimensions = new ArrayList<>();
      dimensions.add(Dimension.builder().name("DBInstanceIdentifier").value(resource.dbInstanceIdentifier()).build());
      Pair<Long, GetMetricStatisticsResponse> freeStorageSpace =
        AWSUtils.getCloudwatchMetricMinimum(data.get("region").asText(), "AWS/RDS", "FreeStorageSpace", dimensions);

      if (freeStorageSpace.getValue0() != null) {
        logger.warn("{} RDS instance is missing engine property", resource.dbInstanceIdentifier());
        AWSUtils.update(data, Map.of("size", Map.of("FreeStorageSpace", freeStorageSpace.getValue0())));

        // pull the relevant node(s) from the payload object. See https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/rds.html
        long freeStorageCapacity = freeStorageSpace.getValue0();
        long storageCapacity = resource.allocatedStorage();

        data.put("sizeInBytes", Conversions.GibToBytes(storageCapacity) - freeStorageCapacity);
        data.put("maxSizeInBytes", Conversions.GibToBytes(storageCapacity));
      } else {
        logger.warn("{} RDS instance is missing size metrics", resource.dbInstanceIdentifier());
      }
    } catch (Exception se) {
      logger.warn("{} RDS instance is missing size metrics, with error {}", resource.dbInstanceIdentifier(), se.getMessage());
    }
  }

  private void setDocDBSize(DBInstance resource, ObjectNode data, Logger logger) {
    try {
      List<Dimension> dimensions = new ArrayList<>();
      dimensions.add(Dimension.builder().name("DBClusterIdentifier").value(resource.dbInstanceIdentifier()).build());
      Pair<Long, GetMetricStatisticsResponse> volumeBytesUsed =
        AWSUtils.getCloudwatchMetricMaximum(data.get("region").asText(), "AWS/DocDB", "VolumeBytesUsed", dimensions);

      if (volumeBytesUsed.getValue0() != null) {
          AWSUtils.update(data, Map.of("size", Map.of("VolumeBytesUsed", volumeBytesUsed.getValue0())));

        data.put("sizeInBytes", volumeBytesUsed.getValue0());
        data.put("maxSizeInBytes", Conversions.GibToBytes(resource.allocatedStorage()));
      }
    } catch (Exception se) {
      logger.warn("{} RDS instance is missing size metrics, with error {}", resource.dbInstanceArn(), se.getMessage());
    }
  }

  private void setAuroraDBSize(DBInstance resource, ObjectNode data, Logger logger) {
    try {
      List<Dimension> dimensions = new ArrayList<>();
      dimensions.add(Dimension.builder().name("DBClusterIdentifier").value(resource.dbInstanceIdentifier()).build());
      Pair<Long, GetMetricStatisticsResponse> volumeBytesUsed =
        AWSUtils.getCloudwatchMetricMaximum(data.get("region").asText(), "AWS/RDS", "VolumeBytesUsed", dimensions);

      if (volumeBytesUsed.getValue0() != null) {
        AWSUtils.update(data, Map.of("size", Map.of("VolumeBytesUsed", volumeBytesUsed.getValue0())));

        data.put("sizeInBytes", volumeBytesUsed.getValue0());
        data.put("maxSizeInBytes", Conversions.GibToBytes(resource.allocatedStorage()));

      }
    } catch (Exception se) {
      logger.warn("{} RDS instance is missing size metrics, with error {}", resource.dbInstanceArn(), se.getMessage());
    }
  }
}
