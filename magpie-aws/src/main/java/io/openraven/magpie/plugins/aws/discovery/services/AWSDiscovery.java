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
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSDiscoveryPlugin;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;

import java.util.List;

public interface AWSDiscovery{

  String service();

  default void discoverWrapper(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger) {
    logger.debug("Starting {} discovery in {}", service(), region);
    discover(mapper, session, region, emitter, logger);
    logger.debug("Completed {} discovery in {}", service(), region);
  }

  void discover(ObjectMapper mapper, Session session, Region region, Emitter Emitter, Logger logger);

  default String fullService() {
    return AWSDiscoveryPlugin.ID + ":"  + service();
  }

  List<Region> getSupportedRegions();
}
