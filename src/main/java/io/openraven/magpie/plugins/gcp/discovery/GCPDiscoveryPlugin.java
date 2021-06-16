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

package io.openraven.magpie.plugins.gcp.discovery;

import com.google.cloud.resourcemanager.Project;
import com.google.cloud.resourcemanager.ResourceManagerOptions;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.OriginPlugin;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.services.*;
import io.sentry.Sentry;
import org.slf4j.Logger;

import java.util.List;


public class GCPDiscoveryPlugin implements OriginPlugin<GCPDiscoveryConfig> {

  public final static String ID = "magpie.gcp.discovery";

  private static final List<GCPDiscovery> DISCOVERY_LIST = List.of(
    new AutoMLDiscovery(),
    new BigQueryDiscovery(),
    new BigTableDiscovery(),
    new ClusterDiscovery(),
    new SecretDiscovery(),
    new RedisDiscovery(),
    new MemcacheDiscovery(),
    new IoTDiscovery(),
    new DataCatalogDiscovery(),
    new TasksDiscovery(),
    new KMSDiscovery(),
    new FunctionsDiscovery());

  GCPDiscoveryConfig config;

  private Logger logger;

  @Override
  public void discover(Session session, Emitter emitter) {
    getProjectList().forEach(project -> DISCOVERY_LIST
      .stream()
      .filter(service -> isEnabled(service.service()))
      .forEach(gcpDiscovery ->
        gcpDiscovery.discoverWrapper(project.getProjectId(), session, emitter, logger)));
  }

  Iterable<Project> getProjectList() {
    var resourceManager = ResourceManagerOptions.getDefaultInstance().getService();
    return resourceManager.list().iterateAll();
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public void init(GCPDiscoveryConfig config, Logger logger) {
    Sentry.init();

    this.logger = logger;
    this.config = config;
  }

  private boolean isEnabled(String service) {
    var enabled = config.getServices().isEmpty() || config.getServices().contains(service);
    logger.debug("{} {} per config", enabled ? "Enabling" : "Disabling", service);
    return enabled;
  }

  @Override
  public Class<GCPDiscoveryConfig> configType() {
    return GCPDiscoveryConfig.class;
  }
}
