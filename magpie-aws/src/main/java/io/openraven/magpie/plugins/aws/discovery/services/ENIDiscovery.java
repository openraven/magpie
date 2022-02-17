package io.openraven.magpie.plugins.aws.discovery.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieAwsResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.aws.ec2.Ec2NetworkInterface;
import io.openraven.magpie.plugins.aws.discovery.AWSDiscoveryPlugin;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesRequest;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class ENIDiscovery implements AWSDiscovery {

  private static final String SERVICE = "eni";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {
    try (final var client = clientCreator.apply(Ec2Client.builder()).build()) {
      discoverNetworkInterfaces(mapper, session, client, region, emitter, account);
    }
  }

  private void discoverNetworkInterfaces(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = Ec2NetworkInterface.RESOURCE_TYPE;

    try {
      client.describeNetworkInterfacesPaginator(DescribeNetworkInterfacesRequest.builder().build()).networkInterfaces().stream()
        .forEach(networkInterface -> {
          String arn = format("arn:aws:ec2:%s:%s:network-interface/%s", region, account, networkInterface.networkInterfaceId());
          var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, arn)
            .withResourceName(networkInterface.networkInterfaceId())
            .withResourceId(networkInterface.networkInterfaceId())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(networkInterface.toBuilder()))
            .withAccountId(account)
            .withAwsRegion(region.toString())
            .withTags(getConvertedTags(networkInterface.tagSet(), mapper))
            .build();

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":NetworkInterface"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private JsonNode getConvertedTags(List<Tag> tags, ObjectMapper mapper) {
    return mapper.convertValue(tags.stream().collect(
      Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
  }

  @Override
  public List<Region> getSupportedRegions() {
    return Ec2Client.serviceMetadata().regions();
  }

}
