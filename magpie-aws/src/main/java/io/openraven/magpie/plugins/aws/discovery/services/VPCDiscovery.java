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
import io.openraven.magpie.api.MagpieAwsResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSDiscoveryPlugin;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeFlowLogsRequest;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Vpc;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;
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
        String arn = format("arn:aws:ec2:%s:%s:vpc/%s", region, account, vpc.vpcId());
        var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, arn)
          .withResourceName(vpc.vpcId())
          .withResourceId(vpc.vpcId())
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(vpc.toBuilder()))
          .withAccountId(account)
          .withAwsRegion(region.toString())
          .withTags(getConvertedTags(vpc.tags(), mapper))
          .build();

        discoverFlowLogs(client, data, vpc);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService()), data.toJsonNode()));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverFlowLogs(Ec2Client client, MagpieAwsResource data, Vpc vpc) {
    final String keyname = "flowLogs";
    var flowLogsFilter = Filter.builder().name("resource-id").values(List.of(vpc.vpcId())).build();

    getAwsResponse(
      () -> client.describeFlowLogs(DescribeFlowLogsRequest.builder().filter(flowLogsFilter).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverVpcPeeringConnections(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::EC2::VPCPeeringConnection";

    try {
      client.describeVpcPeeringConnectionsPaginator().vpcPeeringConnections().forEach(vpcPC -> {
        String arn = format("arn:aws:ec2:%s:%s:vpc-peering-connection/%s", region, account, vpcPC.vpcPeeringConnectionId());
        var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, arn)
          .withResourceName(vpcPC.vpcPeeringConnectionId())
          .withResourceId(vpcPC.vpcPeeringConnectionId())
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(vpcPC.toBuilder()))
          .withAccountId(account)
          .withAwsRegion(region.toString())
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
