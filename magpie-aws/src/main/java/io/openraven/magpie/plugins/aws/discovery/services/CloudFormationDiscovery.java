package io.openraven.magpie.plugins.aws.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieAwsResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.aws.cloudfront.CloudFrontDistribution;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;

import java.util.List;

public class CloudFormationDiscovery implements AWSDiscovery {

  private static final String SERVICE = "cloudFormation";


  @Override
  public String service() {return SERVICE;}

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter Emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {
    final String RESOURCE_TYPE = CloudFrontDistribution.RESOURCE_TYPE;

//    try (final var client = clientCreator.apply(CloudFormation.builder()).build()) {
//      client.listDistributions().distributionList().items().forEach(distribution -> {
//        var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, distribution.arn())
//          .withResourceName(distribution.domainName())
//          .withResourceId(distribution.id())
//          .withResourceType(RESOURCE_TYPE)
//          .withConfiguration(mapper.valueToTree(distribution.toBuilder()))
//          .withAccountId(account)
//          .withAwsRegion(region.toString())
//          .build();
//
//        discoverTags(client, distribution, data, mapper);
//
//        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":distribution"), data.toJsonNode()));
//      });
//    } catch (SdkServiceException | SdkClientException ex) {
//      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
//    }

  }

  @Override
  public List<Region> getSupportedRegions() {
    return null;
  }

}
