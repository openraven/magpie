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
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gcp.storage.StorageBucket;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class StorageDiscovery implements GCPDiscovery {
  private static final String SERVICE = "storage";

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
      var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, bucket.getName())
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withRegion(bucket.getLocation().toLowerCase())
        .withConfiguration(GCPUtils.asJsonNode(bucket))
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
