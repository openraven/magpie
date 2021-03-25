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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.openraven.magpie.plugins.aws.discovery.AWSDiscoveryPlugin;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import org.javatuples.Pair;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.BucketLocationConstraint;
import software.amazon.awssdk.services.s3.model.GetBucketAclRequest;
import software.amazon.awssdk.services.s3.model.GetBucketAclResponse;
import software.amazon.awssdk.services.s3.model.GetBucketEncryptionRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLoggingRequest;
import software.amazon.awssdk.services.s3.model.GetBucketMetricsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketNotificationConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyStatusRequest;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyStatusResponse;
import software.amazon.awssdk.services.s3.model.GetBucketReplicationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.GetBucketWebsiteRequest;
import software.amazon.awssdk.services.s3.model.GetObjectLockConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetPublicAccessBlockRequest;
import software.amazon.awssdk.services.s3.model.Tag;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;
import static java.util.Arrays.asList;

public class S3Discovery implements AWSDiscovery {

  private static String SERVICE = "s3";

  // This is required due to the way S3 bucket data is implemented in the AWS SDK.  Finding the region for n-buckets
  // requires n+1 API calls, and you can't filter bucket lists by region.  Using this cache we perform this operation once
  // per session and cache it for a fixed number of minutes.
  private static final Cache<String, Map<Region, List<Bucket>>> bucketCache = CacheBuilder.newBuilder()
    .expireAfterAccess(Duration.ofMinutes(20))
    .build();

  private final List<LocalDiscovery> discoveryMethods = asList(
    this::discoverEncryption,
    this::discoverHosting,
    this::discoverACLS,
    this::discoverPublicAccess,
    this::discoverLogging,
    this::discoverMetrics,
    this::discoverNotifications,
    this::discoverBucketPolicy,
    this::discoverObjectLockConfiguration,
    this::discoverReplication,
    this::discoverPublic,
    this::discoverVersioning,
    this::discoverBucketTags,
    this::discoverSize
  );

  @FunctionalInterface
  interface LocalDiscovery {
    void discover(S3Client client, Bucket resource, ObjectNode data, Logger logger, ObjectMapper mapper);
  }

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger) {
    final var client = S3Client.builder().region(region).build();
    final var bucketOpt = getbuckets(session, client, region, logger);
    if (bucketOpt.isEmpty()) {
      logger.debug("No buckets found for {}", region);
    }

    bucketOpt.get().forEach( bucket -> {
      var data = mapper.createObjectNode();
      data.put("region", region.toString());
      for (var dm : discoveryMethods) {
        try {
          dm.discover(client, bucket, data, logger, mapper);
        } catch (SdkServiceException | SdkClientException ex) {
          logger.error("Failed to discover dat for {}", bucket.name(), ex);
        }
      }
      emitter.emit(new MagpieEnvelope(session, List.of(fullService()), data));
    });
    logger.info("Finished S3 bucket discovery in {}", region);
  }

  private Optional<List<Bucket>> getbuckets(Session session, S3Client client, Region bucketRegion, Logger logger) {
    try {
      var buckets = bucketCache.get(session.getId(), new Callable<Map<Region, List<Bucket>>>() {

        //
        // This method is executed whenever the bucket cache does not contain entries for a given session ID.  This ensures
        // that we only make this expensive computation the lesser of once per scan or once per timeout period (defined above).
        //
        @Override
        public Map<Region, List<Bucket>> call() {
          logger.debug("No cache found for {}, creating one now.", session);
          var map = new HashMap<Region, List<Bucket>>();
          client.listBuckets().buckets().stream().forEach(bucket -> {
            final var resp = client.getBucketLocation(GetBucketLocationRequest.builder().bucket(bucket.name()).build());
            final var location = resp.locationConstraint();
            // Thanks to https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/model/GetBucketLocationResponse.html#locationConstraint--
            // we need to be aware of both null and UNKNOWN_TO_SDK_VERSION values.
            if (location == BucketLocationConstraint.UNKNOWN_TO_SDK_VERSION) {
              logger.warn("Unknown region {} for bucket {}, ignoring", resp.locationConstraintAsString(), bucket.name());
              return;
            }
            logger.debug("Associating {} to region {}", bucket.name(), location);
            var region = location == null ? Region.US_EAST_1 : Region.of(location.toString());
            var list = map.getOrDefault(region, new LinkedList<>());
            list.add(bucket);
            map.put(region, list);
          });
          return map;
        }
      });
      return Optional.ofNullable(buckets.get(bucketRegion));
    } catch (ExecutionException ex) {
      throw new RuntimeException("S3 Discovery failed", ex);
    }
  }




  private void discoverPublic(S3Client client, Bucket resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    boolean isPublicByACL = false;
    boolean isPublicByPolicy = false;

    // wrap into a try/catch so that if there isn't an ACL response we catch it, default to false, and continue
    try {
      GetBucketAclResponse bucketAcl =
        client.getBucketAcl(GetBucketAclRequest.builder()
          .bucket(resource.name())
          .build());

      isPublicByACL = bucketAcl.grants().stream()
        .anyMatch(grant -> "http://acs.amazonaws.com/groups/global/AllUsers".equalsIgnoreCase(grant.grantee().uri()) ||
          "http://acs.amazonaws.com/groups/global/AuthenticatedUsers".equalsIgnoreCase(grant.grantee().uri()));
    }
    catch (SdkServiceException ex){
      if (!(ex.statusCode() == 403 || ex.statusCode() == 404)) {
        throw ex;
      }
    }

    // wrap into a try/catch so that if there isn't an policy status response we catch it, default to false, and continue
    try {
      GetBucketPolicyStatusResponse bucketPolicyStatus =
        client.getBucketPolicyStatus(GetBucketPolicyStatusRequest.builder()
          .bucket(resource.name())
          .build());

      isPublicByPolicy = bucketPolicyStatus.policyStatus().isPublic();
    }
    catch (SdkServiceException ex){
      if (!(ex.statusCode() == 403 || ex.statusCode() == 404)) {
        throw ex;
      }
    }

    AWSUtils.update(data,
      Map.of("isPublic", isPublicByACL || isPublicByPolicy,
        "isPublicByACL", isPublicByACL,
        "isPublicByPolicy", isPublicByPolicy));
  }

  private void discoverACLS(S3Client client, Bucket resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    final String keyname = "bucketACLConfiguration";
    getAwsResponse(
      () -> client.getBucketAcl(GetBucketAclRequest.builder().bucket(resource.name()).build()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );

  }

  private void discoverEncryption(S3Client client, Bucket resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    final String keyname = "serverSideEncryptionConfiguration";
    getAwsResponse(
      () -> client.getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(resource.name()).build()).serverSideEncryptionConfiguration(),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private void discoverVersioning(S3Client client, Bucket resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    final String keyname = "versioning";
    getAwsResponse(
      () -> client.getBucketVersioning(GetBucketVersioningRequest.builder().bucket(resource.name()).build()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data,
        Map.of(keyname, noresp))
    );
  }

  private void discoverHosting(S3Client client, Bucket resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    final String keyname = "bucketWebsiteConfiguration";
    getAwsResponse(
      () -> client.getBucketWebsite(GetBucketWebsiteRequest.builder().bucket(resource.name()).build()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );

  }

  private void discoverObjectLockConfiguration(S3Client client, Bucket resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    final String keyname = "bucketObjectLockConfiguration";
    getAwsResponse(
      () -> client.getObjectLockConfiguration(GetObjectLockConfigurationRequest.builder().bucket(resource.name()).build()).objectLockConfiguration(),
      (resp) ->
        AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );

  }

  private void discoverLogging(S3Client client, Bucket resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    final String keyname = "bucketLoggingConfiguration";
    getAwsResponse(
      () -> client.getBucketLogging(GetBucketLoggingRequest.builder().bucket(resource.name()).build()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );

  }

  private void discoverMetrics(S3Client client, Bucket resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    final String keyname = "metricsConfiguration";
    getAwsResponse(
      () -> client.getBucketMetricsConfiguration(GetBucketMetricsConfigurationRequest.builder().bucket(resource.name()).build()).metricsConfiguration(),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );

  }

  private void discoverNotifications(S3Client client, Bucket resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    final String keyname = "notificationConfiguration";
    getAwsResponse(
      () -> client.getBucketNotificationConfiguration(GetBucketNotificationConfigurationRequest.builder().bucket(resource.name()).build()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );

  }

  private void discoverPublicAccess(S3Client client, Bucket resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    final String keyname = "publicAccessBlockConfiguration";
    getAwsResponse(
      () -> client.getPublicAccessBlock(GetPublicAccessBlockRequest.builder().bucket(resource.name()).build()).publicAccessBlockConfiguration(),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );

  }

  private void discoverBucketPolicy(S3Client client, Bucket resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    final String keyname = "bucketPolicyStatus";
    getAwsResponse(
      () -> client.getBucketPolicyStatus(GetBucketPolicyStatusRequest.builder().bucket(resource.name()).build()).policyStatus(),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );

    final String keyname2 = "bucketPolicy";
    getAwsResponse(
      () -> client.getBucketPolicy(GetBucketPolicyRequest.builder().bucket(resource.name()).build()).policy(),
      (resp) -> AWSUtils.update(data, Map.of(keyname2, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname2, noresp))
    );


  }

  private void discoverReplication(S3Client client, Bucket resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    final String keyname = "replicationConfiguration";
    getAwsResponse(
      () -> client.getBucketReplication(GetBucketReplicationRequest.builder().bucket(resource.name()).build()).replicationConfiguration(),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private void discoverBucketTags(S3Client client, Bucket resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    var obj = data.putObject("tags");
    getAwsResponse(
      () -> client.getBucketTagging(GetBucketTaggingRequest.builder().bucket(resource.name()).build()),
      (resp) -> {
        JsonNode tagsNode = mapper.convertValue(resp.tagSet().stream()
          .collect(Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
        AWSUtils.update(obj, tagsNode);
      },
      (noresp) -> AWSUtils.update(obj, noresp)
    );
  }

  private void discoverSize(S3Client client, Bucket resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    List<Dimension> dimensions = new ArrayList<>();
    dimensions.add(Dimension.builder().name("bucketName").value(resource.name()).build());
    dimensions.add(Dimension.builder().name("storageType").value("StandardStorage").build());
    Pair<Long, GetMetricStatisticsResponse> bucketSizeBytes =
      AWSUtils.getCloudwatchMetricMaximum(data.get("region").asText(), "AWS/S3", "BucketSizeBytes", dimensions);

    List<Dimension> dimensions2 = new ArrayList<>();
    dimensions2.add(Dimension.builder().name("bucketName").value(resource.name()).build());
    dimensions2.add(Dimension.builder().name("storageType").value("AllStorageTypes").build());
    Pair<Long, GetMetricStatisticsResponse> numberOfObjects =
      AWSUtils.getCloudwatchMetricMaximum(data.get("region").asText(), "AWS/S3", "NumberOfObjects", dimensions2);

    if (numberOfObjects.getValue0() != null && bucketSizeBytes.getValue0() != null) {
      AWSUtils.update(data,
        Map.of("size",
          Map.of("bucketSizeBytes", bucketSizeBytes.getValue0(),
            "numberOfObjects", numberOfObjects.getValue0())));

      data.put("sizeInBytes", bucketSizeBytes.getValue0());
    }
  }
}
