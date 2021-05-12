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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.OriginPlugin;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.services.SecretManagerDiscovery;
import io.sentry.Sentry;
import org.slf4j.Logger;


public class GCPDiscoveryPlugin implements OriginPlugin<GCPDiscoveryConfig> {

  protected static final ObjectMapper MAPPER = new ObjectMapper()
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .findAndRegisterModules();

  public final static String ID = "magpie.gcp.discovery";

  @Override
  public void discover(Session session, Emitter emitter) {
    SecretManagerDiscovery secretManagerDiscovery = new SecretManagerDiscovery();
    secretManagerDiscovery.discover(MAPPER, session, emitter);
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public void init(GCPDiscoveryConfig config, Logger logger) {
    Sentry.init();
  }

  @Override
  public Class<GCPDiscoveryConfig> configType() {
    return GCPDiscoveryConfig.class;
  }
}
