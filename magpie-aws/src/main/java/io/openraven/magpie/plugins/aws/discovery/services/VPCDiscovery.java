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
import io.openraven.magpie.api.MagpieResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSDiscoveryPlugin;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
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
    final var client = Ec2Client.builder().region(region).build();

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
        String arn = format("arn:aws:ec2:%s:%s:vpc/%s", region, account, vpc.vpcId());
        var data = new MagpieResource.MagpieResourceBuilder(mapper, arn)
          .withResourceName(vpc.vpcId())
          .withResourceId(vpc.vpcId())
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(vpc))
          .withAccountId(account)
          .withRegion(region.toString())
          .withTags(getConvertedTags(vpc.tags(), mapper))
          .build();

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService()), data.toJsonNode()));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverVpcPeeringConnections(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::EC2::VPCPeeringConnection";

    try {
      client.describeVpcPeeringConnectionsPaginator().vpcPeeringConnections().forEach(vpcPC -> {
        String arn = format("arn:aws:ec2:%s:%s:vpc-peering-connection/%s", region, account, vpcPC.vpcPeeringConnectionId());
        var data = new MagpieResource.MagpieResourceBuilder(mapper, arn)
          .withResourceName(vpcPC.vpcPeeringConnectionId())
          .withResourceId(vpcPC.vpcPeeringConnectionId())
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(vpcPC))
          .withAccountId(account)
          .withRegion(region.toString())
          .withTags(getConvertedTags(vpcPC.tags(), mapper))
          .build();

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":vpcpc"), data.toJsonNode()));
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
