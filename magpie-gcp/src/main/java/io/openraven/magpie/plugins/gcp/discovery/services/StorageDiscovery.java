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
import com.google.cloud.Policy;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.storage.v2.BucketName;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gcp.storage.StorageBucket;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
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
    try {
        if(maybeCredentialsProvider.isPresent()){
            builder.setCredentials(maybeCredentialsProvider.get().getCredentials());
        }
    }catch(IOException ioException) {
        throw new RuntimeException(ioException);
    }
    Storage storage = builder.setProjectId(projectId).build().getService();
    storage.list().iterateAll().forEach(bucket -> {

      var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, String.format(
        ASSET_ID_FORMAT, BucketName.format(projectId, bucket.getName())
      ))
        .withProjectId(projectId)
        .withResourceId(bucket.getName())
        .withResourceType(RESOURCE_TYPE)
        .withRegion(bucket.getLocation().toLowerCase())
        // Get BucketInfo object instead, this contains the core set of properties and removes nasty bits
        // like the current request data that contain tokens.
        // NOTE: this method is in beta since 2.14.0 (which is a very long time ago, we are on major version 26 now).
        // Should this ever deprecate, we can migrate by using the BucketInfo builder and manually set the fields
        .withConfiguration(GCPUtils.asJsonNode(bucket.asBucketInfo()))
        .build();

      discoverBucketPolicy(data, bucket);

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":bucket"), data.toJsonNode()));
    });
  }

  private void discoverBucketPolicy(MagpieGcpResource data, Bucket bucket) {
    String fieldName = "iamPolicy";
    Policy iamPolicy = bucket.getStorage().getIamPolicy(bucket.getName(), Storage.BucketSourceOption.requestedPolicyVersion(3));
    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, iamPolicy));
  }
}
