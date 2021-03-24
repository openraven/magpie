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
import io.openraven.magpie.plugins.aws.discovery.AWSDiscoveryPlugin;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.*;
import software.amazon.awssdk.services.kms.model.Tag;

import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;
import static java.util.Arrays.asList;

public class KMSDiscovery implements AWSDiscovery {
  private final List<LocalDiscovery> discoveryMethods = asList(
    this::discoverKeyRotation,
    this::discoverAliases,
    this::discoverKeyPolicies,
    this::discoverGrants,
    this::discoverTags
  );

  @FunctionalInterface
  interface LocalDiscovery {
    void discover(KmsClient client, KeyListEntry resource, ObjectNode data, Logger logger, ObjectMapper mapper);
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger) {
    logger.info("Discovering KMS instances in {}", region);
    final var client = KmsClient.builder().region(region).build();

    try {
      client.listKeysPaginator().keys().forEach(key -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", key.toBuilder());
        data.put("region", region.toString());

        for (var dm : discoveryMethods) {
          try {
            dm.discover(client, key, data, logger, mapper);
          } catch (SdkServiceException | SdkClientException ex) {
            logger.error("Failed to discover data for {}", key.keyArn(), ex);
          }
        }

        emitter.emit(new MagpieEnvelope(session, List.of(AWSDiscoveryPlugin.ID + ":kms"), data));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      logger.error("Failed to discover data in {}", region, ex);
    }

    logger.info("Finished KMS instances discovery in {}", region);
  }

  private void discoverKeyRotation(KmsClient client, KeyListEntry resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    logger.info("Getting Rotation for {}", resource.keyArn());
    final String keyname = "Rotation";
    getAwsResponse(
      () -> client.getKeyRotationStatus(GetKeyRotationStatusRequest.builder().keyId(resource.keyId()).build()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private void discoverAliases(KmsClient client, KeyListEntry resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    logger.info("Getting Aliases for {}", resource.keyArn());
    final String keyname = "Aliases";
    getAwsResponse(
      () -> client.listAliases(ListAliasesRequest.builder().keyId(resource.keyId()).build()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private void discoverKeyPolicies(KmsClient client, KeyListEntry resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    logger.info("Getting KeyPolicies for {}", resource.keyArn());
    final String keyname = "KeyPolicies";
    getAwsResponse(
      () -> client.listKeyPolicies(ListKeyPoliciesRequest.builder().keyId(resource.keyId()).build()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private void discoverGrants(KmsClient client, KeyListEntry resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    logger.info("Getting Grants for {}", resource.keyArn());
    final String keyname = "Grants";
    getAwsResponse(
      () -> client.listGrants(ListGrantsRequest.builder().keyId(resource.keyId()).build()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private void discoverTags(KmsClient client, KeyListEntry resource, ObjectNode data, Logger logger, ObjectMapper mapper) {
    logger.info("Getting Tags for {}", resource.keyArn());
    var obj = data.putObject("tags");
    getAwsResponse(
      () -> client.listResourceTags(ListResourceTagsRequest.builder().keyId(resource.keyId()).build()),
      (resp) -> {
        JsonNode tagsNode = mapper.convertValue(resp.tags().stream()
          .collect(Collectors.toMap(Tag::tagKey, Tag::tagValue)), JsonNode.class);
        AWSUtils.update(obj, tagsNode);
      },
      (noresp) -> AWSUtils.update(obj, noresp)
    );
  }
}
