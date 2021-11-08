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

package io.openraven.magpie.plugins.aws.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.OriginPlugin;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.services.*;
import io.sentry.Sentry;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AWSDiscoveryPlugin implements OriginPlugin<AWSDiscoveryConfig> {

  public final static String ID = "magpie.aws.discovery";
  protected static final ObjectMapper MAPPER = new ObjectMapper()
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .findAndRegisterModules();

  private static final List<AWSDiscovery> DISCOVERY_LIST = List.of(
    new AthenaDiscovery(),
    new BatchDiscovery(),
    new CassandraDiscovery(),
    new BackupDiscovery(),
    new CloudFrontDiscovery(),
    new CloudSearchDiscovery(),
    new CloudTrailDiscovery(),
    new CloudWatchDiscovery(),
    new CloudWatchLogsDiscovery(),
    new ConfigDiscovery(),
    new DynamoDbDiscovery(),
    new EBDiscovery(),
    new EC2Discovery(),
    new ECSDiscovery(),
    new EFSDiscovery(),
    new EKSDiscovery(),
    new ElastiCacheDiscovery(),
    new ELBDiscovery(),
    new ELBV2Discovery(),
    new EMRDiscovery(),
    new ESDiscovery(),
    new FSXDiscovery(),
    new GlacierDiscovery(),
    new GuardDutyDiscovery(),
    new IAMDiscovery(),
    new LakeFormationDiscovery(),
    new LambdaDiscovery(),
    new LightsailDiscovery(),
    new LocationDiscovery(),
    new QLDBDiscovery(),
    new S3Discovery(),
    new SecretsManagerDiscovery(),
    new SecurityHubDiscovery(),
    new SNSDiscovery(),
    new SSMDiscovery(),
    new StorageGatewayDiscovery(),
    new RDSDiscovery(),
    new RedshiftDiscovery(),
    new Route53Discovery(),
    new KMSDiscovery(),
    new VPCDiscovery());

  private Logger logger;
  private AWSDiscoveryConfig config;

  @Override
  public void discover(Session session, Emitter emitter) {

    final var enabledPlugins = DISCOVERY_LIST.stream().filter(p -> isEnabled(p.service())).collect(Collectors.toList());

    if (config.getAssumedRoles() == null || config.getAssumedRoles().isEmpty()) {
      final var account = StsClient.create().getCallerIdentity().account();
      enabledPlugins.forEach(plugin -> {
        final var regions = getRegionsForDiscovery(plugin);
        regions.forEach(region -> {
          try {
            final var clientCreator = ClientCreators.localClientCreator(region);
            plugin.discoverWrapper(MAPPER, session, region, emitter, logger, account, clientCreator);
          } catch (Exception ex) {
            logger.error("Discovery error  in {} - {}", region.id(), ex.getMessage());
            logger.debug("Details", ex);
          }
        });
      });
    } else {
      config.getAssumedRoles().forEach(role -> {
        enabledPlugins.forEach(plugin -> {
          final var regions = getRegionsForDiscovery(plugin);
          regions.forEach(region -> {
            final var clientCreator = ClientCreators.assumeRoleCreator(region, role);
            try (final var client = clientCreator.apply(StsClient.builder()).build()) {
              final String account = client.getCallerIdentity().account();
              logger.info("Discovering cross-account {}:{} using role {}", plugin.service(), region,   role);
              plugin.discoverWrapper(MAPPER, session, region, emitter, logger, account, clientCreator);
            } catch (Exception ex) {
              logger.error("Discovery error  in {} - {}", region.id(), ex.getMessage());
              logger.debug("Details", ex);
            }
          });
        });
      });
    }
  }

  protected List<Region> getRegionsForDiscovery(AWSDiscovery plugin) {
    final var regions = plugin.getSupportedRegions()
      .stream()
      .filter(region -> isDiscoveryEnabledIn(region.toString()))
      .filter(region -> isAllowedRegion(region.toString()))
      .collect(Collectors.toList());

    if (regions.isEmpty()) {
      logger.warn("{} is enabled but no supported regions are configured.", plugin.fullService());
    }
    return regions;
  }

  private boolean isEnabled(String svc) {
    var enabled = config.getServices().isEmpty() || config.getServices().contains(svc);
    logger.debug("{} {} per config", enabled ? "Enabling" : "Disabling", svc);
    return enabled;
  }

  private boolean isDiscoveryEnabledIn(String region) {
    var enabled = config.getRegions().isEmpty() || config.getRegions().contains(region);
    logger.debug("{} {} per config", enabled ? "Enabling" : "Disabling", region);
    return enabled;
  }

  private boolean isAllowedRegion(String region) {
    boolean regionAllowed = config.getIgnoredRegions()
      .stream()
      .noneMatch(pattern -> Pattern.matches(pattern, region));
    logger.debug("{} {} per ignore region config", regionAllowed ? "Enabling" : "Disabling", region);
    return regionAllowed;
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public void init(AWSDiscoveryConfig config, Logger logger) {
    this.logger = logger;
    this.config = config;

    Sentry.init();
  }

  @Override
  public Class<AWSDiscoveryConfig> configType() {
    return AWSDiscoveryConfig.class;
  }
}
