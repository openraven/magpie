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
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSResource;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class KMSDiscovery implements AWSDiscovery {

  private static final String SERVICE = "kms";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return KmsClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    final var client = KmsClient.builder().region(region).build();
    final String RESOURCE_TYPE = "AWS::Kms::Key";

    try {
      client.listKeysPaginator().keys().forEach(key -> {
        var data = new AWSResource(key.toBuilder(), region.toString(), account, mapper);
        data.arn = key.keyArn();
        data.resourceId = key.keyId();
        data.resourceName = key.toString();
        data.resourceType = RESOURCE_TYPE;

        discoverKeyRotation(client, key, data);
        discoverAliases(client, key, data);
        discoverKeyPolicies(client, key, data);
        discoverGrants(client, key, data);
        discoverTags(client, key, data, mapper);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService()), data.toJsonNode(mapper)));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex, session.getId());
    }
  }

  private void discoverKeyRotation(KmsClient client, KeyListEntry resource, AWSResource data) {
    final String keyname = "rotation";
    getAwsResponse(
      () -> client.getKeyRotationStatus(GetKeyRotationStatusRequest.builder().keyId(resource.keyId()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverAliases(KmsClient client, KeyListEntry resource, AWSResource data) {
    final String keyname = "aliases";
    getAwsResponse(
      () -> client.listAliasesPaginator(ListAliasesRequest.builder().keyId(resource.keyId()).build()).aliases()
        .stream()
        .map(AliasListEntry::toBuilder)
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverKeyPolicies(KmsClient client, KeyListEntry resource, AWSResource data) {
    final String keyname = "keyPolicies";
    getAwsResponse(
      () -> client.listKeyPolicies(ListKeyPoliciesRequest.builder().keyId(resource.keyId()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverGrants(KmsClient client, KeyListEntry resource, AWSResource data) {
    final String keyname = "grants";
    getAwsResponse(
      () -> client.listGrants(ListGrantsRequest.builder().keyId(resource.keyId()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverTags(KmsClient client, KeyListEntry resource, AWSResource data, ObjectMapper mapper) {
    getAwsResponse(
      () -> client.listResourceTags(ListResourceTagsRequest.builder().keyId(resource.keyId()).build()),
      (resp) -> {
        JsonNode tagsNode = mapper.convertValue(resp.tags().stream()
          .collect(Collectors.toMap(Tag::tagKey, Tag::tagValue)), JsonNode.class);
        AWSUtils.update(data.tags, tagsNode);
      },
      (noresp) -> AWSUtils.update(data.tags, noresp)
    );
  }
}
