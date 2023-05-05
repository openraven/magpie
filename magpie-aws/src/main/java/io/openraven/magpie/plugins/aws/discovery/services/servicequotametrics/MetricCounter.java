package io.openraven.magpie.plugins.aws.discovery.services.servicequotametrics;

import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;

public interface MetricCounter {

  String quotaCode();

  Number count(MagpieAWSClientCreator clientCreator);
}
