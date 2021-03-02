package io.openraven.nightglow.plugins.aws.discovery;

import io.openraven.nightglow.api.Emitter;
import io.openraven.nightglow.api.EnumerationPlugin;
import io.openraven.nightglow.api.Session;
import io.openraven.nightglow.plugins.aws.discovery.services.EC2Discovery;
import org.slf4j.Logger;
import software.amazon.awssdk.services.ec2.model.Region;

public class AWSDiscoveryPlugin implements EnumerationPlugin<AWSDiscoveryConfig> {

  public final static String ID = "nightglow.aws.discovery";

  private Logger logger;
  private AWSDiscoveryConfig config;

  @Override
  public void discover(Session session, Emitter emitter) {
    Region.Builder;
    Region.builder().
    new EC2Discovery()
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public void init(AWSDiscoveryConfig config, Logger logger) {
    this.logger = logger;
    this.config = config;
  }

  @Override
  public Class<AWSDiscoveryConfig> configType() {
    return AWSDiscoveryConfig.class;
  }
}
