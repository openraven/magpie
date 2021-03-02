package io.openraven.nightglow.plugins.aws.discovery.services;

import io.openraven.nightglow.api.Emitter;
import io.openraven.nightglow.api.Session;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

public interface AWSDiscovery {

  void discover(Session session, Ec2Client client, Region region, Emitter emitter, Logger logger);
}
