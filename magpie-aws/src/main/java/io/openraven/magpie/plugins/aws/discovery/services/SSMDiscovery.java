package io.openraven.magpie.plugins.aws.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;

import java.util.List;

import static java.lang.String.format;

public class SSMDiscovery implements AWSDiscovery {

  private static final String SERVICE = "ssm";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return SsmClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    SsmClient client = AWSUtils.configure(SsmClient.builder(), region);

    final String RESOURCE_TYPE = "AWS::SSM::Instance";

    try {
      client.describeInstanceInformationPaginator().instanceInformationList().forEach(instance -> {
        String arn = format("arn:aws:ec2:%s:instance/%s", region, instance.instanceId());
        var data = new MagpieResource.MagpieResourceBuilder(mapper, arn)
          .withResourceName(instance.instanceId())
          .withResourceId(instance.instanceId())
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(instance.toBuilder()))
          .withAccountId(account)
          .withRegion(region.toString())
          .build();

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService()), data.toJsonNode()));

      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }
}
