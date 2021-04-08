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
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSDiscoveryPlugin;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.VersioningEmitterWrapper;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.util.List;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class VPCDiscovery implements AWSDiscovery {

  private static final String SERVICE = "vpc";

  @FunctionalInterface
  interface LocalDiscovery {
    void discover(ObjectMapper mapper, Session session, Ec2Client client, Region region, VersioningEmitterWrapper emitter, Logger logger);
  }

  public void discover(ObjectMapper mapper, Session session, Region region, VersioningEmitterWrapper emitter, Logger logger) {
    final var client = Ec2Client.builder().region(region).build();
    final List<LocalDiscovery> methods = List.of(
      this::discoverVpcs,
      this::discoverVpcPeeringConnections
    );

    methods.forEach(m -> m.discover(mapper, session, client, region, emitter, logger));
  }

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return Ec2Client.serviceMetadata().regions();
  }

  private void discoverVpcs(ObjectMapper mapper, Session session, Ec2Client client, Region region, VersioningEmitterWrapper emitter, Logger logger) {
    getAwsResponse(
      client::describeVpcsPaginator,
      (resp) -> resp.vpcs()
        .forEach(vpc -> {
          ObjectNode data = mapper.createObjectNode();
          data.putPOJO("configuration", vpc.toBuilder());
          var obj = data.putObject("tags");
          AWSUtils.update(obj, getConvertedTags(vpc.tags(), mapper));

          emitter.emit(new MagpieEnvelope(session, List.of(fullService()), data));
        }),
      (noresp) -> logger.debug("Couldn't query for VPCs in {}.", region));
  }

  private void discoverVpcPeeringConnections(ObjectMapper mapper, Session session,  Ec2Client client, Region region, VersioningEmitterWrapper emitter, Logger logger) {
    getAwsResponse(
      client::describeVpcPeeringConnectionsPaginator,
      (resp) -> resp.vpcPeeringConnections()
        .forEach(vcpPC -> {
          ObjectNode data = mapper.createObjectNode();
          data.putPOJO("configuration", vcpPC.toBuilder());
          var obj = data.putObject("tags");
          AWSUtils.update(obj, getConvertedTags(vcpPC.tags(), mapper));

          emitter.emit(new MagpieEnvelope(session, List.of(AWSDiscoveryPlugin.ID + ":vpcpc"), data));
        }),
      (noresp) -> logger.debug("Couldn't query for VPC peering connections in {}.", region));
  }

  private JsonNode getConvertedTags(List<Tag> tags, ObjectMapper mapper) {
    return mapper.convertValue(tags.stream().collect(
      Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
  }
}
