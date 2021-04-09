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
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.HostedZone;
import software.amazon.awssdk.services.route53.model.ListResourceRecordSetsRequest;
import software.amazon.awssdk.services.route53.model.ListTrafficPolicyInstancesByHostedZoneRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class Route53Discovery implements AWSDiscovery {

  private static final String SERVICE = "route53";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return Route53Client.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger) {
    final var client = Route53Client.builder().region(region).build();

    getAwsResponse(
      () -> client.listHostedZonesPaginator().hostedZones().stream(),
      (resp) -> resp.forEach(hostedZone -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", hostedZone.toBuilder());
        data.put("region", region.toString());

        discoverRecordSets(client, hostedZone, data);
        discoverTrafficPolicyInstances(client, hostedZone, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":hostedZone"), data));
      }),
      (noresp) -> logger.error("Failed to get hostedZones in {}", region)
    );
  }

  private void discoverRecordSets(Route53Client client, HostedZone resource, ObjectNode data) {
    final String keyname = "recordSets";

    getAwsResponse(
      () -> client.listResourceRecordSetsPaginator(ListResourceRecordSetsRequest.builder().hostedZoneId(resource.id()).build()).resourceRecordSets()
        .stream()
        .map(r -> r.toBuilder())
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private void discoverTrafficPolicyInstances(Route53Client client, HostedZone resource, ObjectNode data) {
    final String keyname = "trafficPolicyInstances";

    getAwsResponse(
      () -> client.listTrafficPolicyInstancesByHostedZone(ListTrafficPolicyInstancesByHostedZoneRequest.builder().hostedZoneId(resource.id()).build()).trafficPolicyInstances(),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }
}
