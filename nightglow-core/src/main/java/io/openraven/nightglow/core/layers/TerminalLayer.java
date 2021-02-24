package io.openraven.nightglow.core.layers;

import io.openraven.nightglow.api.TerminalPlugin;
import io.openraven.nightglow.core.fifos.FifoDequeue;
import io.openraven.nightglow.core.fifos.FifoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class TerminalLayer implements Layer {

  private final static Logger LOGGER = LoggerFactory.getLogger(TerminalLayer.class);

  private final FifoDequeue dequeue;
  private final Collection<TerminalPlugin<?>> plugins;


  public TerminalLayer(FifoDequeue dequeue, Collection<TerminalPlugin<?>> plugins) {
    this.dequeue = dequeue;
    this.plugins = plugins;
  }

  @Override
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
        p.accept(env);
      } catch (Exception ex) {
        LOGGER.warn("Plugin exception: {}", p.id(), ex);
      }
    });
  }
}
