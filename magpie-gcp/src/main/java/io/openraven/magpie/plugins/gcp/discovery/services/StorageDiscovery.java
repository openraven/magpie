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

package io.openraven.magpie.plugins.gcp.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.CredentialsProvider;
import com.google.appengine.repackaged.com.google.common.base.Pair;
import com.google.cloud.Identity;
import com.google.cloud.Policy;
import com.google.cloud.Role;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.cloud.monitoring.v3.MetricServiceSettings;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import com.google.monitoring.v3.ListTimeSeriesRequest;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeInterval;
import com.google.protobuf.util.Timestamps;
import com.google.storage.v2.BucketName;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gcp.storage.StorageBucket;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StorageDiscovery implements GCPDiscovery {
  private static final Logger LOGGER = LoggerFactory.getLogger(StorageDiscovery.class);
  private static final String SERVICE = "storage";
  public static final String ASSET_ID_FORMAT = "//storage.googleapis.com/%s";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger, Optional<CredentialsProvider> maybeCredentialsProvider) {
    final String RESOURCE_TYPE = StorageBucket.RESOURCE_TYPE;

    final StorageOptions.Builder builder = StorageOptions.newBuilder();
    final var mBuilder = MetricServiceSettings.newBuilder();

    try {
        if(maybeCredentialsProvider.isPresent()){
            builder.setCredentials(maybeCredentialsProvider.get().getCredentials());
            mBuilder.setCredentialsProvider(maybeCredentialsProvider.get());
        }
    } catch(IOException ioException) {
        throw new RuntimeException(ioException);
    }

    final var sizeMap = new HashMap<String, Double>();
    final var countMap = new HashMap<String, Long>();

    final Storage storage = builder.setProjectId(projectId).build().getService();
    try (MetricServiceClient metrics = MetricServiceClient.create(mBuilder.build())) {
      sizeMap.putAll(queryTotalBytes(metrics, projectId));
      countMap.putAll(queryTotalObjects(metrics, projectId));
    } catch (Exception ex) {
      LOGGER.debug("Metrics discovery exception: {}", ex.getMessage());
    }

    storage.list().iterateAll().forEach(bucket -> {
      var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, String.format(
        ASSET_ID_FORMAT, BucketName.format(projectId, bucket.getName())
      ))
        .withProjectId(projectId)
        .withResourceId(bucket.getName())
        .withResourceType(RESOURCE_TYPE)
        .withRegion(bucket.getLocation().toLowerCase())
        .withCreatedIso(bucket.getCreateTimeOffsetDateTime().toInstant())
        .withRegion(bucket.getLocation())
        // Get BucketInfo object instead, this contains the core set of properties and removes nasty bits
        // like the current request data that contain tokens.
        // NOTE: this method is in beta since 2.14.0 (which is a very long time ago, we are on major version 26 now).
        // Should this ever deprecate, we can migrate by using the BucketInfo builder and manually set the fields
        .withConfiguration(GCPUtils.asJsonNode(bucket.asBucketInfo()))
        .build();

      try {
        final var iamPolicy = bucket.getStorage().getIamPolicy(bucket.getName(), Storage.BucketSourceOption.requestedPolicyVersion(3));
        final var iamConfiguration = bucket.getIamConfiguration();
        discoverBucketPolicy(data, bucket, iamPolicy);
        discoverPublicAccessPrevention(data, iamConfiguration);
        discoverPublicHosting(data, iamPolicy, iamConfiguration);
      } catch (StorageException ex) {
        if (ex.getMessage().contains("does not have storage.buckets.getIamPolicy")) {
          LOGGER.debug("Could not access IAM Policy for {}: {}", bucket.getName(), ex.getMessage());
        } else {
          DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, ex);
        }
      }

      discoverLabels(data, bucket);
      discoverBucketEncryption(data, bucket);
      discoverSizeMetrics(data, bucket, sizeMap, countMap);

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":bucket"), data.toJsonNode()));
    });

  }

//  private Map<String, String> discoverTags(Bucket bucket) throws GeneralSecurityException, IOException {
//    JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
//    final var transport = GoogleNetHttpTransport.newTrustedTransport();
//
//    final var tagManager = new TagManager.Builder(transport, jsonFactory, ).build();
//
//
//
//    return Map.of();
//  }

  private void discoverLabels(MagpieGcpResource data, Bucket bucket) {
    GCPUtils.update(data.supplementaryConfiguration, Pair.of("labels", bucket.getLabels()));
  }

  private void discoverPublicHosting(MagpieGcpResource data, Policy iamPolicy, BucketInfo.IamConfiguration iamConfiguration) {
    //
    // Public hosting is considered on if both of the following are true:
    // 1) There's an ACL entry allowing 'allUsers' the 'Storage Object Viewer' permission
    // AND
    // 2) publicAccessPrevention is disabled.  Note that we are currently unable to check the value of INHERITED for this,
    // so we'll assume the worst case if it's not explicitly ENFORCED.
    //
    // See https://cloud.google.com/storage/docs/hosting-static-website for details
    final var identities = iamPolicy.getBindings().get(Role.of("roles/storage.objectViewer"));
    final var allAccess = identities != null && identities.contains(Identity.allUsers());

    String fieldName = "publicHosting";
    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, allAccess && iamConfiguration.getPublicAccessPrevention() != BucketInfo.PublicAccessPrevention.ENFORCED));
  }

  private void discoverPublicAccessPrevention(MagpieGcpResource data, BucketInfo.IamConfiguration iamConfiguration) {
    String fieldName = "publicAccessPrevention";
    final var publicAccessPrevention = iamConfiguration.getPublicAccessPrevention();
    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, publicAccessPrevention));
  }

  private void discoverBucketEncryption(MagpieGcpResource data, Bucket bucket) {
    var fieldName = "defaultKmsKeyName";
    final var keyName = bucket.getDefaultKmsKeyName();
    GCPUtils.update(data.supplementaryConfiguration, Pair.of("isEncrypted", true));  // GCP always encrypts blobs in storage, the only question is whether it's a Google or customer managed key
    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, keyName));   // Non-null if it's a customer-managed key, null if it's Google managed.
  }

  private void discoverBucketPolicy(MagpieGcpResource data, Bucket bucket, Policy iamPolicy) {
    String fieldName = "iamPolicy";
    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, iamPolicy));
  }

  private Map<String, Long> queryTotalObjects(MetricServiceClient client, java.lang.String projectId) {
    // Measured once per day, but has a 300s sample rate. We don't need multiple values so let's just get the
    // last 300 seconds.
    final long now = System.currentTimeMillis();
    var interval =
      TimeInterval.newBuilder()
        .setStartTime(Timestamps.fromMillis(now-600000L))
        .setEndTime(Timestamps.fromMillis(now))
        .build();

    //
    // Object Count
    //
    var request = ListTimeSeriesRequest.newBuilder()
      .setName(ProjectName.of(projectId).toString())
      .setInterval(interval)
      .setFilter("metric.type=\"storage.googleapis.com/storage/object_count\"")
      .build();

    var response = client.listTimeSeries(request);

    var map = new HashMap<String, Long>();

    response.iterateAll().forEach( t -> {
      if (t.hasMetric() && !t.getPointsList().isEmpty()) {
        // We only care about the first data point, as it's only updated once every 24 hours
        var point = t.getPoints(0);
        try {
          map.put(t.getResource().getLabelsOrThrow("bucket_name"), point.getValue().getInt64Value());
        } catch (IllegalArgumentException ex) {
          LOGGER.debug("Couldn't find bucket_name in the data point resource info.");
        }
      }
    });
    return map;
  }


  private Map<String, Double> queryTotalBytes(MetricServiceClient client, java.lang.String projectId) {
    // Measured once per day, but has a 300s sample rate. We don't need multiple values so let's just get the
    // last 300 seconds.
    final long now = System.currentTimeMillis();
    var interval =
      TimeInterval.newBuilder()
        .setStartTime(Timestamps.fromMillis(now-600000L))
        .setEndTime(Timestamps.fromMillis(now))
        .build();

    var request = ListTimeSeriesRequest.newBuilder()
      .setName(ProjectName.of(projectId).toString())
      .setInterval(interval)
      .setFilter("metric.type=\"storage.googleapis.com/storage/total_bytes\"")
      .build();

    var response = client.listTimeSeries(request);
    var map = new HashMap<String, Double>();

    response.iterateAll().forEach( t -> {
      if (t.hasMetric() && !t.getPointsList().isEmpty()) {
        // We only care about the first data point, as it's only updated once every 24 hours
        var point = t.getPoints(0);
        try {
          map.put(t.getResource().getLabelsOrThrow("bucket_name"), point.getValue().getDoubleValue());
        } catch (IllegalArgumentException ex) {
          LOGGER.debug("Couldn't find bucket_name in the data point resource info.");
        }
      }
    });
    return map;
  }

  private void discoverSizeMetrics(MagpieGcpResource data, Bucket bucket, Map<String, Double> sizeMap, Map<String, Long> countMap) {
    final String fieldName = "size";
    final String name = bucket.getName();
    final var sizeValue = sizeMap.containsKey(name) ? Long.toString(sizeMap.get(name).longValue()) : "";
    final var countValue = countMap.containsKey(name) ? countMap.get(name).toString() : "";

    if (!sizeValue.isEmpty()) {
      data.sizeInBytes = Long.parseLong(sizeValue);
    }

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName,
      Map.of(
        "BucketSizeBytes", sizeValue,
        "NumberOfObjects", countValue
      )));
  }
}
