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

package io.openraven.magpie.core.layers;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.openraven.magpie.api.MagpiePlugin;
import io.openraven.magpie.api.OriginPlugin;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.core.fifos.FifoException;
import io.openraven.magpie.core.fifos.FifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class OriginLayer implements Layer {

  private final static Logger LOGGER = LoggerFactory.getLogger(OriginLayer.class);
  private final static ObjectMapper MAPPER = new ObjectMapper();
  private final static ObjectMapper ANNOTATED_MAPPER = new ObjectMapper()
    .registerModule(new JavaTimeModule())
    .findAndRegisterModules()
    .activateDefaultTyping(
      LaissezFaireSubTypeValidator.instance,
      ObjectMapper.DefaultTyping.NON_FINAL,
      JsonTypeInfo.As.WRAPPER_ARRAY
    )
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

  private final Session session;
  private final Collection<OriginPlugin> plugins;
  private final FifoQueue queue;
  private final String name;

  public OriginLayer(String name, Session session, Collection<OriginPlugin> plugins, FifoQueue queue) {
    this.session = session;
    this.plugins = plugins;
    this.queue = queue;
    this.name = name;
  }

  @Override
  public void exec() throws FifoException {
    plugins.forEach(p -> {
      try {
        LOGGER.trace("Running discover for plugin {}", p.id());
        p.discover(session, this::emit);
      } catch (Exception ex) {
        LOGGER.warn("Plugin exception: {}", p.id(), ex);
      }
    });
  }

  @Override
  public String getName() {
    return name;
  }

  private void emit(MagpieEnvelope env) {
    try {
      // Rather than set these values on *each* service discovery, we set it centrally here.
      env.getContents().set("discoveryMeta", ANNOTATED_MAPPER.valueToTree(session));
      env.getContents().set("discoverySessionId", MAPPER.valueToTree(session.getId()));
      env.setSession(session);
      queue.add(env);
    } catch (FifoException e) {
      LOGGER.warn("Emitter exception", e);
    }
  }

  @Override
  public LayerType getType() {
    return LayerType.ORIGIN;
  }

  @Override
  public void shutdown() {
    plugins.forEach(MagpiePlugin::shutdown);
  }
}
