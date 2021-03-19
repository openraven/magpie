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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSDiscoveryPlugin;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.util.List;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class EC2Discovery implements AWSDiscovery {

  @FunctionalInterface
  interface LocalDiscovery {
    void discover(ObjectMapper mapper, Session session,  Ec2Client client, Region region, Emitter emitter, Logger logger);
  }

  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger) {
    final var client = Ec2Client.builder().region(region).build();
    final List<LocalDiscovery> methods = List.of(
      (m, ds, cl, r, em, l) -> discoverEc2Instances(mapper, session, client, region, emitter, logger),
      (m, ds, cl, r, em, l) -> discoverEIPs(mapper, session, client, region, emitter, logger),
      (m, ds, cl, r, em, l) -> discoverSecurityGroups(mapper, session, client, region, emitter, logger),
      (m, ds, cl, r, em, l) -> discoverVolumes(mapper, session, client, region, emitter, logger)
    );

    methods.forEach(m -> m.discover(mapper, session, client, region, emitter, logger));
  }

  private void discoverEc2Instances(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, Logger logger) {
    logger.info("Discovering EC2 Instances in {}", region);

    getAwsResponse(
      client::describeInstancesPaginator,
      (resp) -> resp.stream()
        .flatMap(r -> r.reservations().stream())
        .forEach(i -> emitter.emit(new MagpieEnvelope(session, List.of(AWSDiscoveryPlugin.ID + ":ec2"), mapper.valueToTree(i.toBuilder())))),
      (noresp) -> logger.debug("Couldn't query for EC2 Instances in {}.", region));

    logger.info("Finished EC2 Instance discovery in {}", region);
  }

  private void discoverEIPs(ObjectMapper mapper, Session session,  Ec2Client client, Region region, Emitter emitter, Logger logger) {

    logger.info("Discovering EIPs in {}", region);

    getAwsResponse(
      client::describeAddresses,
      (resp) -> resp.addresses()
        .forEach(addr -> emitter.emit(new MagpieEnvelope(session, List.of(AWSDiscoveryPlugin.ID + ":eip"), mapper.valueToTree(addr.toBuilder())))),
      (noresp) -> logger.debug("Couldn't query for EIPs in {}.", region));

    logger.info("Finished EIP discovery in {}", region);
  }

  private void discoverSecurityGroups(ObjectMapper mapper, Session session,  Ec2Client client, Region region, Emitter emitter, Logger logger) {
    logger.info("Discovering SecurityGroups in {}", region);

    getAwsResponse(
      client::describeSecurityGroupsPaginator,
      (resp) -> resp.stream()
        .flatMap(r -> r.securityGroups().stream())
        .forEach(sg -> emitter.emit(new MagpieEnvelope(session, List.of(AWSDiscoveryPlugin.ID + ":sg"), mapper.valueToTree(sg.toBuilder())))),
      (noresp) -> logger.debug("Couldn't query for SecurityGroups in {}.", region));

    logger.info("Finished SecurityGroup discovery in {}", region);
  }

  private void discoverVolumes(ObjectMapper mapper, Session session,  Ec2Client client, Region region, Emitter emitter, Logger logger) {
    logger.info("Discovering Volumes in {}", region);

    getAwsResponse(
      client::describeVolumesPaginator,
      (resp) -> resp.stream()
        .flatMap(r -> r.volumes().stream())
        .forEach(v -> emitter.emit(new MagpieEnvelope(session, List.of(AWSDiscoveryPlugin.ID + ":volume"), mapper.valueToTree(v.toBuilder())))),
      (noresp) -> logger.debug("Couldn't query for Volumes in {}.", region));

    logger.info("Finished Volumes discovery in {}", region);
  }
}
