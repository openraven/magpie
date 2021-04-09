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
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
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
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger) {
    final var client = StorageGatewayClient.builder().region(region).build();

    getAwsResponse(
      () ->  client.listGatewaysPaginator().gateways().stream(),
      (resp) -> resp.forEach(gateway -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", gateway.toBuilder());
        data.put("region", region.toString());

        discoverGatewayInfo(client, gateway, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":gateway"), data));
      }),
      (noresp) -> logger.error("Failed to get gateways in {}", region)
    );
  }

  private void discoverGatewayInfo(StorageGatewayClient client, GatewayInfo resource, ObjectNode data) {
    final String keyname = "gatewayInfo";

    getAwsResponse(
      () -> client.describeGatewayInformation(DescribeGatewayInformationRequest.builder().gatewayARN(resource.gatewayARN()).build()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }
}
