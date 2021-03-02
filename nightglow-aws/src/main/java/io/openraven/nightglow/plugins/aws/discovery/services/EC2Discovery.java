package io.openraven.nightglow.plugins.aws.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.nightglow.api.Emitter;
import io.openraven.nightglow.api.NGEnvelope;
import io.openraven.nightglow.api.Session;
import io.openraven.nightglow.plugins.aws.discovery.AWSDiscoveryPlugin;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.util.List;

import static io.openraven.nightglow.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class EC2Discovery implements AWSDiscovery {

  @FunctionalInterface
  interface LocalDiscovery {
    void discover(Session session, Ec2Client client, Emitter emitter);
  }

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final Ec2Client client;
  private final Region region;
  private final Logger logger;

  public EC2Discovery(Ec2Client client, Region region, Logger logger) {
    this.client = client;
    this.region = region;
    this.logger = logger;
  }

  public void discover(Session session,  Emitter emitter) {
    final List<LocalDiscovery> methods = List.of(
      (ds, cl, em) -> discoverEc2Instances(session, client, emitter),
      (ds, cl, em) -> discoverEIPs(session, client, emitter),
      (ds, cl, em) -> discoverSecurityGroups(session, client, emitter),
      (ds, cl, em) -> discoverVolumes(session, client, emitter)
    );

    methods.forEach(m -> m.discover(session, client, emitter));
  }


  private void discoverEc2Instances(Session session, Ec2Client client, Emitter emitter) {
    logger.info("Discovering EC2 Instances in {}", region);

    getAwsResponse(
      client::describeInstancesPaginator,
      (resp) -> resp.stream()
        .flatMap(r -> r.reservations().stream())
        .forEach(i -> emitter.emit(
          new NGEnvelope(session, List.of(AWSDiscoveryPlugin.ID + ":ec2"), MAPPER.valueToTree(i.toBuilder()))
        )),
      (noresp) -> logger.debug("Couldn't query for EC2 Instances in {}.", region));

    logger.info("Finished EC2 Instance discovery in {}", region);
  }

  private void discoverEIPs(Session session, Ec2Client client, Emitter emitter) {

    logger.info("Discovering EIPs in {}", region);

    getAwsResponse(
      client::describeAddresses,
      (resp) -> resp.addresses()
        .forEach(addr -> emitter.emit(
          new NGEnvelope(session, List.of(AWSDiscoveryPlugin.ID + ":eip"), MAPPER.valueToTree(addr.toBuilder()))
        )),
      (noresp) -> logger.debug("Couldn't query for EIPs in {}.", region));

    logger.info("Finished EIP discovery in {}", region);
  }

  private void discoverSecurityGroups(Session session, Ec2Client client, Emitter emitter) {
    logger.info("Discovering SecurityGroups in {}", region);

    getAwsResponse(
      client::describeSecurityGroupsPaginator,
      (resp) -> resp.stream()
        .flatMap(r -> r.securityGroups().stream())
        .forEach(sg -> emitter.emit(
          new NGEnvelope(session, List.of(AWSDiscoveryPlugin.ID + ":sg"), MAPPER.valueToTree(sg.toBuilder()))
        )),
      (noresp) -> logger.debug("Couldn't query for SecurityGroups in {}.", region));

    logger.info("Finished SecurityGroup discovery in {}", region);
  }

  private void discoverVolumes(Session discoverySession, Ec2Client client, Emitter emitter) {
    logger.info("Discovering Volumes in {}", region);

    getAwsResponse(
      client::describeVolumesPaginator,
      (resp) -> resp.stream()
        .flatMap(r -> r.volumes().stream())
        .forEach(v -> emitter.emit(
          new NGEnvelope(discoverySession, List.of(AWSDiscoveryPlugin.ID + ":volume"), MAPPER.valueToTree(v.toBuilder()))
        )),
      (noresp) -> logger.debug("Couldn't query for Volumes in {}.", region));

    logger.info("Finished Volumes discovery in {}", region);
  }

}
