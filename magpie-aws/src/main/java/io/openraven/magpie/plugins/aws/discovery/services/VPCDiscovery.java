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
import io.openraven.magpie.plugins.aws.discovery.*;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class VPCDiscovery implements AWSDiscovery {

  private static final String SERVICE = "vpc";

  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    final var client = AWSUtils.configure(Ec2Client.builder(), region);

    discoverVpcs(mapper, session, client, region, emitter, account);
    discoverVpcPeeringConnections(mapper, session, client, region, emitter, account);
  }

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return Ec2Client.serviceMetadata().regions();
  }

  private void discoverVpcs(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::EC2::VPC";

    try {
      client.describeVpcsPaginator().vpcs().forEach(vpc -> {
        var data = new AWSResource(vpc.toBuilder(), region.toString(), account, mapper);
        data.arn = format("arn:aws:ec2:%s:%s:vpc/%s", region, account, vpc.vpcId());
        data.resourceId = vpc.vpcId();
        data.resourceName = vpc.vpcId();
        data.resourceType = RESOURCE_TYPE;
        data.tags = getConvertedTags(vpc.tags(), mapper);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService()), data.toJsonNode(mapper)));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverVpcPeeringConnections(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::EC2::VPCPeeringConnection";

    try {
      client.describeVpcPeeringConnectionsPaginator().vpcPeeringConnections().forEach(vpcPC -> {
        var data = new AWSResource(vpcPC.toBuilder(), region.toString(), account, mapper);
        data.arn = format("arn:aws:ec2:%s:%s:vpc-peering-connection/%s", region, account, vpcPC.vpcPeeringConnectionId());
        data.resourceId = vpcPC.vpcPeeringConnectionId();
        data.resourceName = vpcPC.vpcPeeringConnectionId();
        data.resourceType = RESOURCE_TYPE;
        data.tags = getConvertedTags(vpcPC.tags(), mapper);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":vpcpc"), data.toJsonNode(mapper)));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private JsonNode getConvertedTags(List<Tag> tags, ObjectMapper mapper) {
    return mapper.convertValue(tags.stream().collect(
      Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
  }
}
