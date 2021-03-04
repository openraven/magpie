package io.openraven.nightglow.core.layers;

import io.openraven.nightglow.api.EnumerationPlugin;
import io.openraven.nightglow.api.IntermediatePlugin;
import io.openraven.nightglow.api.NightglowPlugin;
import io.openraven.nightglow.api.Session;
import io.openraven.nightglow.api.TerminalPlugin;
import io.openraven.nightglow.core.config.ConfigException;
import io.openraven.nightglow.core.config.LayerConfig;
import io.openraven.nightglow.core.config.NightglowConfig;
import io.openraven.nightglow.core.fifos.FifoDequeue;
import io.openraven.nightglow.core.fifos.FifoManager;
import io.openraven.nightglow.core.fifos.FifoQueue;
import io.openraven.nightglow.core.plugins.PluginManager;
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

  private final NightglowConfig config;
  private final FifoManager fifoManager;
  private final Map<String, Layer> layers = new LinkedHashMap<>();  // Preserve insertion order



  public LayerManager(Session session, NightglowConfig config, FifoManager fifoManager, PluginManager pluginManager) {
    this.config = config;
    this.fifoManager = fifoManager;
    buildLayers(session, fifoManager, pluginManager);
  }

  private void buildLayers(Session session, FifoManager fifoManager, PluginManager pluginManager) {
    config.getLayers().forEach((name, layerConfig) -> {
      List<NightglowPlugin> plugins = layerConfig.getPlugins().stream()
        .map(pluginManager::byId)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());

      final var layerType = LayerType.valueOf(layerConfig.getType().toUpperCase());
      switch (layerType) {
        case ORIGIN:
          layers.put(name, new OriginLayer(name, session,
            plugins.stream().map(p -> (EnumerationPlugin)p).collect(Collectors.toList()),
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
