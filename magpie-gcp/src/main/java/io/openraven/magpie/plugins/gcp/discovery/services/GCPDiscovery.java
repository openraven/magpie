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

package io.openraven.magpie.plugins.gcp.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.CredentialsProvider;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.GCPDiscoveryPlugin;
import org.slf4j.Logger;

import java.util.Optional;

public interface GCPDiscovery {
  String service();

  default void discoverWrapper(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger, Optional<CredentialsProvider> maybeCredentialsProvider) {
    logger.debug("Starting {} discovery ", service());
    discover(mapper, projectId, session, emitter, logger, maybeCredentialsProvider);
    logger.debug("Completed {} discovery", service());
  }

  void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger, Optional<CredentialsProvider> maybeCredentialsProvider);

  default String fullService() {
    return GCPDiscoveryPlugin.ID + ":" + service();
  }
}

