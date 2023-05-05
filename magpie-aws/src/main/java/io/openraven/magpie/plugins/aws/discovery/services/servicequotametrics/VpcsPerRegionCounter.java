package io.openraven.magpie.plugins.aws.discovery.services.servicequotametrics;

import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import software.amazon.awssdk.services.ec2.Ec2Client;

public class VpcsPerRegionCounter implements MetricCounter {

  @Override
  public String quotaCode() {
    return "L-F678F1CE";
  }

  @Override
  public Number count(MagpieAWSClientCreator clientCreator) {
    try (final var client = clientCreator.apply(Ec2Client.builder()).build()) {
      return client.describeVpcsPaginator().stream().count();
    }
  }
}
