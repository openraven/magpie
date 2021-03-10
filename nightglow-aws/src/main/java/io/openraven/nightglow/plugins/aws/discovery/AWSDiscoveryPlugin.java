package io.openraven.nightglow.plugins.aws.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.openraven.nightglow.api.Emitter;
import io.openraven.nightglow.api.EnumerationPlugin;
import io.openraven.nightglow.api.Session;
import io.openraven.nightglow.plugins.aws.discovery.services.AWSDiscovery;
import io.openraven.nightglow.plugins.aws.discovery.services.EC2Discovery;
import io.openraven.nightglow.plugins.aws.discovery.services.S3Discovery;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.util.List;

public class AWSDiscoveryPlugin implements EnumerationPlugin<AWSDiscoveryConfig> {

  protected static final ObjectMapper MAPPER = new ObjectMapper()
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .findAndRegisterModules();

  public final static String ID = "nightglow.aws.discovery";

  private static final List<AWSDiscovery> DISCOVERY_LIST = List.of(new EC2Discovery(), new S3Discovery());

  private Logger logger;
  private AWSDiscoveryConfig config;

  @Override
  public void discover(Session session, Emitter emitter) {
    Ec2Client.create().describeRegions().regions().stream().map(r -> Region.of(r.regionName())).forEach(region -> {
      DISCOVERY_LIST.forEach(d -> {
        try {
          d.discover(MAPPER, session, region, emitter, logger);
        } catch (Exception ex) {
          logger.error("Discovery error", ex);
        }
      });
    });
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
