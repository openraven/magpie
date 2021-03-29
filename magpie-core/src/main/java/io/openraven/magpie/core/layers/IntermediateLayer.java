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

import io.openraven.magpie.api.IntermediatePlugin;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.api.MagpiePlugin;
import io.openraven.magpie.core.fifos.FifoDequeue;
import io.openraven.magpie.core.fifos.FifoException;
import io.openraven.magpie.core.fifos.FifoQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class IntermediateLayer implements Layer {

  private final static Logger LOGGER = LoggerFactory.getLogger(IntermediateLayer.class);

  private final FifoDequeue dequeue;
  private final Collection<IntermediatePlugin> plugins;
  private final FifoQueue queue;
  private final String name;

  public IntermediateLayer(String name, FifoDequeue dequeue, Collection<IntermediatePlugin> plugins, FifoQueue queue) {
    this.dequeue = dequeue;
    this.plugins = plugins;
    this.queue = queue;
    this.name = name;
  }

  public void exec() throws FifoException {
    final var opt = dequeue.poll();
    if (opt.isEmpty()) {
      return;
    }
    final var env = opt.get();
    final var pluginPath = env.getPluginPath();
    final var lastPlugin = pluginPath.isEmpty() ? null : pluginPath.get(pluginPath.size()-1);
    plugins.forEach(p -> {
      try {
        p.accept(env, this::emit);
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
      queue.add(env);
    } catch (FifoException e) {
      LOGGER.warn("Emitter exception", e);
    }
  }

  @Override
  public LayerType getType() {
    return LayerType.INTERMEDIATE;
  }

  @Override
  public void shutdown() {
    plugins.forEach(MagpiePlugin::shutdown);
  }
}
