package io.openraven.nightglow.core;

import io.openraven.nightglow.api.DiscoveryEnvelope;
import io.openraven.nightglow.api.IntermediatePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class IntermediateLayer implements Layer {

  private final static Logger LOGGER = LoggerFactory.getLogger(IntermediateLayer.class);

  private final FifoDequeue dequeue;
  private final Collection<IntermediatePlugin<?>> plugins;
  private final FifoQueue queue;

  public IntermediateLayer(FifoDequeue dequeue, Collection<IntermediatePlugin<?>> plugins, FifoQueue queue) {
    this.dequeue = dequeue;
    this.plugins = plugins;
    this.queue = queue;
  }

  public void exec() throws FifoException {
    final var env = dequeue.poll();
    final var pluginPath = env.getPluginPath();
    final var lastPlugin = pluginPath.isEmpty() ? null : pluginPath.get(pluginPath.size()-1);
    if (env == null) {
      return;
    }

    plugins.stream().filter(p -> lastPlugin == null || matches(lastPlugin, p.accepts())).forEach(p -> {
      try {
        p.accept(env, this::emit);
      } catch (Exception ex) {
        LOGGER.warn("Plugin exception: {}", p.id(), ex);
      }
    });
  }

  private void emit(DiscoveryEnvelope env) {
    try {
      queue.add(env);
    } catch (FifoException e) {
      LOGGER.warn("Emitter exception", e);
    }
  }
}
