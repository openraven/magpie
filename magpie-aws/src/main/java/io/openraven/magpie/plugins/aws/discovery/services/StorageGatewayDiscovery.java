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
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSResource;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.storagegateway.StorageGatewayClient;
import software.amazon.awssdk.services.storagegateway.model.DescribeGatewayInformationRequest;
import software.amazon.awssdk.services.storagegateway.model.GatewayInfo;

import java.util.List;
import java.util.Map;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class StorageGatewayDiscovery implements AWSDiscovery {

  private static final String SERVICE = "storageGateway";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return StorageGatewayClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    final var client = StorageGatewayClient.builder().region(region).build();
    final String RESOURCE_TYPE = "AWS::StorageGateway::Gateway";

    try {
      client.listGatewaysPaginator().gateways().stream().forEach(gateway -> {
        var data = new AWSResource(gateway.toBuilder(), region.toString(), account, mapper);
        data.arn = gateway.gatewayARN();
        data.resourceName = gateway.gatewayName();
        data.resourceId = gateway.gatewayId();
        data.resourceType = RESOURCE_TYPE;

        discoverGatewayInfo(client, gateway, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":gateway"), data.toJsonNode(mapper)));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex, session.getId());
    }
  }

  private void discoverGatewayInfo(StorageGatewayClient client, GatewayInfo resource, AWSResource data) {
    final String keyname = "gatewayInfo";

    getAwsResponse(
      () -> client.describeGatewayInformation(DescribeGatewayInformationRequest.builder().gatewayARN(resource.gatewayARN()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }
}
