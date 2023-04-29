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

import io.openraven.magpie.api.MagpiePlugin;
import io.openraven.magpie.api.TerminalPlugin;
import io.openraven.magpie.core.fifos.FifoDequeue;
import io.openraven.magpie.core.fifos.FifoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class TerminalLayer implements Layer {

  private final static Logger LOGGER = LoggerFactory.getLogger(TerminalLayer.class);

  private final FifoDequeue dequeue;
  private final Collection<TerminalPlugin> plugins;
  private final String name;

  public TerminalLayer(String name, FifoDequeue dequeue, Collection<TerminalPlugin> plugins) {
    this.dequeue = dequeue;
    this.plugins = plugins;
    this.name = name;
  }

  @Override
  public void exec() throws FifoException {
    final var envelopes = dequeue.drain();
    if (envelopes.isEmpty()) {
      return;
    }
    plugins.forEach(p -> {
      try {
        envelopes.forEach(p::accept);
      } catch (Exception ex) {
        LOGGER.warn("Plugin exception: {}", p.id(), ex);
      }
    });
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public LayerType getType() {
    return LayerType.TERMINAL;
  }

  @Override
  public void shutdown() {
    plugins.forEach(MagpiePlugin::shutdown);
  }
}
