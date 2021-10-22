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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.DescribeSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsRequest;

import java.util.List;

public class SecretsManagerDiscovery implements AWSDiscovery {

  private static final String SERVICE = "secretsManager";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return SecretsManagerClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {
    final var client = clientCreator.apply(SecretsManagerClient.builder()).build();
    final String RESOURCE_TYPE = "AWS::SecretsManager";

    try {
      client.listSecretsPaginator(ListSecretsRequest.builder().build()).stream()
        .forEach(secretsPaginatedResponse -> secretsPaginatedResponse.secretList()
          .stream()
          .map(secretListEntry -> client.describeSecret(DescribeSecretRequest.builder().secretId(secretListEntry.arn()).build()))
          .forEach(secret -> {
            var data = new MagpieResource.MagpieResourceBuilder(mapper, secret.arn())
              .withResourceName(secret.name())
              .withResourceType(RESOURCE_TYPE)
              .withConfiguration(mapper.valueToTree(secret.toBuilder()))
              .withCreatedIso(secret.createdDate())
              .withAccountId(account)
              .withRegion(region.toString())
              .build();

            emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":secret"), data.toJsonNode()));
          }));
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }
}
