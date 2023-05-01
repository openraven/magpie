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
import com.fasterxml.jackson.databind.node.NullNode;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieAwsResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.aws.ec2.RegionResource;
import io.openraven.magpie.plugins.aws.discovery.AWSDiscoveryConfig;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;

import java.util.List;

import static java.lang.String.format;

public class RegionDiscovery implements AWSDiscovery {

  private static final String SERVICE = "region";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator, AWSDiscoveryConfig config) {
    final var arn = format("arn:aws::%s:%s", region.id(), account);
    final var metadata = region.metadata();
    final var resourceName = metadata != null ? metadata.description() : "";

    var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, arn)
      .withResourceName(resourceName)
      .withResourceId(region.id())
      .withResourceType(RegionResource.RESOURCE_TYPE)
      .withConfiguration(NullNode.instance)
      .withAccountId(account)
      .withAwsRegion(region.id())
      .build();

    emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService()), data.toJsonNode()));
  }

  @Override
  public List<Region> getSupportedRegions() {
    return Region.regions();
  }
}
