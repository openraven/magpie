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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieAwsResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.aws.s3.S3Bucket;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.javatuples.Pair;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.GetBucketAclRequest;
import software.amazon.awssdk.services.s3.model.GetBucketAclResponse;
import software.amazon.awssdk.services.s3.model.GetBucketEncryptionRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationRequest;
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
import software.amazon.awssdk.services.s3.model.ServerSideEncryptionRule;
import software.amazon.awssdk.services.s3.model.GetBucketEncryptionResponse;
import software.amazon.awssdk.services.s3.model.PolicyStatus;
import software.amazon.awssdk.services.s3.model.Tag;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class S3Discovery implements AWSDiscovery {

  private static final String SERVICE = "s3";
  private static final String DEFAULT_AWS_US_EAST_1_URI = "https://s3.us-east-1.amazonaws.com";

  // This is required due to the way S3 bucket data is implemented in the AWS SDK.  Finding the region for n-buckets
  // requires n+1 API calls, and you can't filter bucket lists by region.  Using this cache we perform this operation once
  // per session and cache it for a fixed number of minutes.
  private static final Cache<String, Map<Region, List<Bucket>>> bucketCache = CacheBuilder.newBuilder()
    .expireAfterAccess(Duration.ofMinutes(20))
    .build();

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return S3Client.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {

    final String RESOURCE_TYPE = S3Bucket.RESOURCE_TYPE;

    try (final var client = configureS3Client(clientCreator, region)) {
      final var bucketOpt = getBuckets(session.getId() + account, client, region, logger);
      if (bucketOpt.isEmpty()) {
        logger.debug("No buckets found for {}", region);
        return;
      }

      bucketOpt.get().forEach(bucket -> {
        var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, "arn:aws:s3:::" + bucket.name())
          .withResourceName(bucket.name())
          .withResourceId(bucket.name())
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(bucket.toBuilder()))
          .withCreatedIso(bucket.creationDate())
          .withAccountId(account)
          .withAwsRegion(region.toString())
          .build();

        discoverEncryption(client, bucket, data);
        discoverHosting(client, bucket, data);
        discoverACLS(client, bucket, data);
        discoverPublicAccess(client, bucket, data);
        discoverLogging(client, bucket, data);
        discoverMetrics(client, bucket, data);
        discoverNotifications(client, bucket, data);
        discoverBucketPolicy(client, bucket, data, mapper);
        discoverObjectLockConfiguration(client, bucket, data);
        discoverReplication(client, bucket, data);
        discoverPublic(client, bucket, data, logger);
        discoverIsEncrypted(client, bucket, data, logger);
        discoverVersioning(client, bucket, data);
        discoverLifeCycleConfiguration(client, bucket, data);
        discoverBucketTags(client, bucket, data, mapper);
        discoverSize(bucket, data, clientCreator);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":bucket"), data.toJsonNode()));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  /**
   * https://github.com/aws/aws-sdk-cpp/issues/1339#issuecomment-598402493
   * AWS S3 Api conduct US_EAST_1/AWS_GLOBAL as a default region and URI -> s3.amazonaws.com
   * We are unable to query buckets from other regions with the default URI without region enrichment
   */
  private S3Client configureS3Client(MagpieAWSClientCreator clientCreator, Region region) {
    final var builder = clientCreator.apply(S3Client.builder());

    // Remap magpie clients to local environment
    String magpieAwsEndpoint = System.getProperty("MAGPIE_AWS_ENDPOINT");
    if (magpieAwsEndpoint != null) {
      builder.endpointOverride(URI.create(magpieAwsEndpoint));
    } else if (Region.US_EAST_1.equals(region) || Region.AWS_GLOBAL.equals(region)) {
      builder.endpointOverride(URI.create(DEFAULT_AWS_US_EAST_1_URI));
    }
    // Build for region only
    return builder.region(region).build();
  }

  private Optional<List<Bucket>> getBuckets(String cacheKey, S3Client client, Region bucketRegion, Logger logger) {
    try {
      //
      // This method is executed whenever the bucket cache does not contain entries for a given session ID.  This ensures
      // that we only make this expensive computation the lesser of once per scan or once per timeout period (defined above).
      //
      var buckets = bucketCache.get(cacheKey, () -> {
        logger.debug("No cache found for {}, creating one now.", cacheKey);
        var map = new HashMap<Region, List<Bucket>>();
        client.listBuckets().buckets().forEach(bucket -> {
          final var resp = client.getBucketLocation(GetBucketLocationRequest.builder().bucket(bucket.name()).build());
          final var location = resp.locationConstraint();
          // Thanks to https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/model/GetBucketLocationResponse.html#locationConstraint--
          // we need to be aware of both null and UNKNOWN_TO_SDK_VERSION values.
          var region =
            Region.US_EAST_1.toString().equals(resp.locationConstraintAsString())
              || resp.locationConstraintAsString().isEmpty()
              ? Region.US_EAST_1
              : Region.of(location.toString());
          logger.debug("Associating {} to region {}", bucket.name(), region);
          var list = map.getOrDefault(region, new LinkedList<>());
          list.add(bucket);
          map.put(region, list);
        });
        return map;
      });
      return Optional.ofNullable(buckets.get(bucketRegion));
    } catch (ExecutionException ex) {
      throw new RuntimeException("S3 Discovery failed", ex);
    }
  }

  private void discoverPublic(S3Client client, Bucket resource, MagpieAwsResource data, Logger logger) {
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
    } catch (SdkServiceException ex) {
      if (!(ex.statusCode() == 403 || ex.statusCode() == 404)) {
        throw ex;
      }
      logger.warn("Failure on S3 public access discovery, BucketName: {}, Reason: {}", resource.name(), ex.getMessage());
    }

    // wrap into a try/catch so that if there isn't an policy status response we catch it, default to false, and continue
    try {
      GetBucketPolicyStatusResponse bucketPolicyStatus =
        client.getBucketPolicyStatus(GetBucketPolicyStatusRequest.builder()
          .bucket(resource.name())
          .build());

      isPublicByPolicy = Optional.of(bucketPolicyStatus.policyStatus())
        .map(PolicyStatus::isPublic)
        .orElse(false);
    } catch (SdkServiceException ex) {
      if (!(ex.statusCode() == 403 || ex.statusCode() == 404)) {
        throw ex;
      }
      logger.warn("Failure on S3 public access discovery, BucketName: {}, Reason: {}", resource.name(), ex.getMessage());
    }

    AWSUtils.update(data.supplementaryConfiguration,
      Map.of("isPublic", isPublicByACL || isPublicByPolicy,
        "isPublicByACL", isPublicByACL,
        "isPublicByPolicy", isPublicByPolicy));
  }

  private void discoverACLS(S3Client client, Bucket resource, MagpieAwsResource data) {
    final String keyname = "BucketACLConfiguration";
    getAwsResponse(
      () -> client.getBucketAcl(GetBucketAclRequest.builder().bucket(resource.name()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );

  }

  private void discoverEncryption(S3Client client, Bucket resource, MagpieAwsResource data) {
    final String keyname = "ServerSideEncryptionConfiguration";
    getAwsResponse(
      () -> client.getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(resource.name()).build()).serverSideEncryptionConfiguration(),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverIsEncrypted(S3Client client, Bucket resource, MagpieAwsResource data, Logger logger) {
    boolean hasEncryptionConfiguration = false;
    boolean hasBucketEncryptionKeyEnabled = false;
    try {
      GetBucketEncryptionResponse bucketEnc = client.getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(resource.name()).build());
      List<ServerSideEncryptionRule> rules = bucketEnc.serverSideEncryptionConfiguration().rules();
      for (ServerSideEncryptionRule rule : rules) {
        if (rule.bucketKeyEnabled()) {
          hasBucketEncryptionKeyEnabled = true;
          break;
        }
      }
      hasEncryptionConfiguration = true;
    } catch (SdkServiceException ex) {
      if (!(ex.statusCode() == 403 || ex.statusCode() == 404)) {
        throw ex;
      }
      logger.warn("Failure on S3 isEncrypted discovery, BucketName: {}, Reason: {}", resource.name(), ex.getMessage());
    }
    AWSUtils.update(data.supplementaryConfiguration,
      Map.of("isEncrypted", hasEncryptionConfiguration && hasBucketEncryptionKeyEnabled,
        "hasEncryptionConfiguration", hasEncryptionConfiguration,
        "hasBucketEncryptionKeyEnabled", hasBucketEncryptionKeyEnabled));
  }

  private void discoverVersioning(S3Client client, Bucket resource, MagpieAwsResource data) {
    final String keyname = "Versioning";
    getAwsResponse(
      () -> client.getBucketVersioning(GetBucketVersioningRequest.builder().bucket(resource.name()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverHosting(S3Client client, Bucket resource, MagpieAwsResource data) {
    final String keyname = "BucketWebsiteConfiguration";
    getAwsResponse(
      () -> client.getBucketWebsite(GetBucketWebsiteRequest.builder().bucket(resource.name()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );

  }

  private void discoverObjectLockConfiguration(S3Client client, Bucket resource, MagpieAwsResource data) {
    final String keyname = "BucketObjectLockConfiguration";
    getAwsResponse(
      () -> client.getObjectLockConfiguration(GetObjectLockConfigurationRequest.builder().bucket(resource.name()).build()).objectLockConfiguration(),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );

  }

  private void discoverLogging(S3Client client, Bucket resource, MagpieAwsResource data) {
    final String keyname = "BucketLoggingConfiguration";
    getAwsResponse(
      () -> client.getBucketLogging(GetBucketLoggingRequest.builder().bucket(resource.name()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );

  }

  private void discoverMetrics(S3Client client, Bucket resource, MagpieAwsResource data) {
    final String keyname = "MetricsConfiguration";
    getAwsResponse(
      () -> client.getBucketMetricsConfiguration(GetBucketMetricsConfigurationRequest.builder().bucket(resource.name()).build()).metricsConfiguration(),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );

  }

  private void discoverNotifications(S3Client client, Bucket resource, MagpieAwsResource data) {
    final String keyname = "NotificationConfiguration";
    getAwsResponse(
      () -> client.getBucketNotificationConfiguration(GetBucketNotificationConfigurationRequest.builder().bucket(resource.name()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );

  }

  private void discoverPublicAccess(S3Client client, Bucket resource, MagpieAwsResource data) {
    final String keyname = "PublicAccessBlockConfiguration";
    getAwsResponse(
      () -> client.getPublicAccessBlock(GetPublicAccessBlockRequest.builder().bucket(resource.name()).build()).publicAccessBlockConfiguration(),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );

  }

  private void discoverBucketPolicy(S3Client client, Bucket resource, MagpieAwsResource data, ObjectMapper mapper) {
    final String keyname = "BucketPolicyStatus";
    getAwsResponse(
      () -> client.getBucketPolicyStatus(GetBucketPolicyStatusRequest.builder().bucket(resource.name()).build()).policyStatus(),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );

    final String keyname2 = "BucketPolicy";
    getAwsResponse(
      () -> client.getBucketPolicy(GetBucketPolicyRequest.builder().bucket(resource.name()).build()).policy(),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname2, AWSUtils.parsePolicyDocument(mapper, resp))),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname2, noresp))
    );
  }

  private void discoverReplication(S3Client client, Bucket resource, MagpieAwsResource data) {
    final String keyname = "ReplicationConfiguration";
    getAwsResponse(
      () -> client.getBucketReplication(GetBucketReplicationRequest.builder().bucket(resource.name()).build()).replicationConfiguration(),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverLifeCycleConfiguration(S3Client client, Bucket resource, MagpieAwsResource data) {
    final String keyname = "LifecycleConfiguration";
    getAwsResponse(
      () -> client.getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(resource.name()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverBucketTags(S3Client client, Bucket resource, MagpieAwsResource data, ObjectMapper mapper) {
    getAwsResponse(
      () -> client.getBucketTagging(GetBucketTaggingRequest.builder().bucket(resource.name()).build()),
      (resp) -> {
        JsonNode tagsNode = mapper.convertValue(resp.tagSet().stream()
          .collect(Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
        AWSUtils.update(data.tags, tagsNode);
      },
      (noresp) -> AWSUtils.update(data.tags, noresp)
    );
  }

  private void discoverSize(Bucket resource, MagpieAwsResource data, MagpieAWSClientCreator clientCreator) {

    // get the different bucket size metrics available
    List<String> storageTypeDimensions = AWSUtils.getS3AvailableSizeMetrics(data.awsRegion, data.resourceName, clientCreator);

    List<Map<String, Long>> storageTypeMap = new ArrayList<>();

    // run through all the available metrics and make cloudwatch calls to get bucket size
    for (String storageType : storageTypeDimensions) {
      List<Dimension> dimensions = new ArrayList<>();
      dimensions.add(Dimension.builder().name("BucketName").value(resource.name()).build());
      dimensions.add(Dimension.builder().name("StorageType").value(storageType).build());
      Pair<Long, GetMetricStatisticsResponse> bucketSizeBytes =
        AWSUtils.getCloudwatchMetricMaximum(data.awsRegion, "AWS/S3", "BucketSizeBytes", dimensions, clientCreator);

      // we are leaving it boxed due to the insertion into the Map below
      final Long bucketSizeMetric = bucketSizeBytes.getValue0();
      if (bucketSizeMetric != null) {
        storageTypeMap.add(Map.of(storageType, bucketSizeMetric));
      }
    }
    data.supplementaryConfiguration = AWSUtils.update(data.supplementaryConfiguration, Map.of("storageTypeSizeInBytes", storageTypeMap));

    List<Dimension> dimensions = new ArrayList<>();
    dimensions.add(Dimension.builder().name("BucketName").value(resource.name()).build());
    dimensions.add(Dimension.builder().name("StorageType").value("StandardStorage").build());
    Pair<Long, GetMetricStatisticsResponse> bucketSizeBytes =
      AWSUtils.getCloudwatchMetricMaximum(data.awsRegion, "AWS/S3", "BucketSizeBytes", dimensions, clientCreator);

    List<Dimension> dimensions2 = new ArrayList<>();
    dimensions2.add(Dimension.builder().name("BucketName").value(resource.name()).build());
    dimensions2.add(Dimension.builder().name("StorageType").value("AllStorageTypes").build());
    Pair<Long, GetMetricStatisticsResponse> numberOfObjects =
      AWSUtils.getCloudwatchMetricMaximum(data.awsRegion, "AWS/S3", "NumberOfObjects", dimensions2, clientCreator);

    if (numberOfObjects.getValue0() != null && bucketSizeBytes.getValue0() != null) {
      AWSUtils.update(data.supplementaryConfiguration,
        Map.of("size",
          Map.of("BucketSizeBytes", bucketSizeBytes.getValue0(),
            "NumberOfObjects", numberOfObjects.getValue0())));

      data.sizeInBytes = bucketSizeBytes.getValue0();
    }
  }

}
