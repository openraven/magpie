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

import io.openraven.magpie.api.OriginPlugin;
import io.openraven.magpie.api.IntermediatePlugin;
import io.openraven.magpie.api.MagpiePlugin;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.api.TerminalPlugin;
import io.openraven.magpie.core.config.ConfigException;
import io.openraven.magpie.core.config.LayerConfig;
import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.core.fifos.FifoDequeue;
import io.openraven.magpie.core.fifos.FifoManager;
import io.openraven.magpie.core.fifos.FifoQueue;
import io.openraven.magpie.core.plugins.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class LayerManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(LayerManager.class);

  private final MagpieConfig config;
  private final FifoManager fifoManager;
  private final Map<String, Layer> layers = new LinkedHashMap<>();  // Preserve insertion order



  public LayerManager(Session session, MagpieConfig config, FifoManager fifoManager, PluginManager pluginManager) {
    this.config = config;
    this.fifoManager = fifoManager;
    buildLayers(session, fifoManager, pluginManager);
  }

  private void buildLayers(Session session, FifoManager fifoManager, PluginManager pluginManager) {
    config.getLayers().forEach((name, layerConfig) -> {
      List<MagpiePlugin> plugins = layerConfig.getPlugins().stream()
        .map(pluginManager::byId)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());

      final var layerType = LayerType.valueOf(layerConfig.getType().toUpperCase());
      switch (layerType) {
        case ORIGIN:
          layers.put(name, new OriginLayer(name, session,
            plugins.stream().map(p -> (OriginPlugin)p).collect(Collectors.toList()),
            getOrThrowQueue(name, layerConfig)));
          break;
        case INTERMEDIATE:
          layers.put(name, new IntermediateLayer(name,
            getOrThrowDequeue(name, layerConfig),
            plugins.stream().map(p -> (IntermediatePlugin)p).collect(Collectors.toList()),
            getOrThrowQueue(name, layerConfig)));
          break;
        case TERMINAL:
          layers.put(name, new TerminalLayer(name,
            getOrThrowDequeue(name, layerConfig),
            plugins.stream().map(p -> (TerminalPlugin)p).collect(Collectors.toList())));
          break;
        default:
          throw new ConfigException(String.format("Illegal type for layer %s: %s", name, layerConfig.getType()));
      }

      LOGGER.debug("Built layer {}", name);
    });
  }

  private FifoQueue getOrThrowQueue(String layerName, LayerConfig layerConfig) {
    final var queueName = layerConfig.getQueue();
    if (Objects.isNull(queueName)) {
      throw new ConfigException("No fifo queue defined for " + layerName);
    }
    var queue = fifoManager.getQueue(queueName);
    if (Objects.isNull(queue)) {
      throw new ConfigException("Couldn't find queue " + queueName);
    }

    return queue;
  }

  private FifoDequeue getOrThrowDequeue(String layerName, LayerConfig layerConfig) {
    final var dequeueName = layerConfig.getDequeue();
    if (Objects.isNull(dequeueName)) {
      throw new ConfigException("No fifo dequeue defined for " + layerName);
    }

    var dequeue = fifoManager.getDequeue(dequeueName);
    if (Objects.isNull(dequeue)) {
      throw new ConfigException("Couldn't find dequeue " + dequeueName);
    }
    return dequeue;
  }

  public Map<String, Layer> getLayers() {
    return Collections.unmodifiableMap(layers);
  }
}
