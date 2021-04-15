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
import io.openraven.magpie.plugins.aws.discovery.AWSDiscoveryPlugin;
import io.openraven.magpie.plugins.aws.discovery.AWSResource;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.util.List;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;
import static java.lang.String.format;

public class VPCDiscovery implements AWSDiscovery {

  private static final String SERVICE = "vpc";

  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    final var client = Ec2Client.builder().region(region).build();

    discoverVpcs(mapper, session, client, region, emitter, logger, account);
    discoverVpcPeeringConnections(mapper, session, client, region, emitter, logger, account);
  }

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return Ec2Client.serviceMetadata().regions();
  }

  private void discoverVpcs(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, Logger logger, String account) {
    getAwsResponse(
      client::describeVpcsPaginator,
      (resp) -> resp.vpcs()
        .forEach(vpc -> {
          var data = new AWSResource(vpc.toBuilder(), region.toString(), account, mapper);
          data.arn = format("arn:aws:ec2:%s:%s:vpc/%s", region.toString(), account, vpc.vpcId());
          data.resourceId = vpc.vpcId();
          data.resourceName = vpc.vpcId();
          data.resourceType = "AWS::EC2::VPC";
          data.tags = getConvertedTags(vpc.tags(), mapper);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService()), data.toJsonNode(mapper)));
        }),
      (noresp) -> logger.debug("Couldn't query for VPCs in {}.", region));
  }

  private void discoverVpcPeeringConnections(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, Logger logger, String account) {
    getAwsResponse(
      client::describeVpcPeeringConnectionsPaginator,
      (resp) -> resp.vpcPeeringConnections()
        .forEach(vcpPC -> {
          var data = new AWSResource(vcpPC.toBuilder(), region.toString(), account, mapper);
          data.arn = format("arn:aws:ec2:%s:%s:vpc-peering-connection/%s", region.toString(), account, vcpPC.vpcPeeringConnectionId());
          data.resourceId = vcpPC.vpcPeeringConnectionId();
          data.resourceName = vcpPC.vpcPeeringConnectionId();
          data.resourceType = "AWS::EC2::VPCPeeringConnection";
          data.tags = getConvertedTags(vcpPC.tags(), mapper);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":vpcpc"), data.toJsonNode(mapper)));
        }),
      (noresp) -> logger.debug("Couldn't query for VPC peering connections in {}.", region));
  }

  private JsonNode getConvertedTags(List<Tag> tags, ObjectMapper mapper) {
    return mapper.convertValue(tags.stream().collect(
      Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
  }
}
