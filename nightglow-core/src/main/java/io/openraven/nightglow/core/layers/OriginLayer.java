package io.openraven.nightglow.core.layers;

import io.openraven.nightglow.api.EnumerationPlugin;
import io.openraven.nightglow.api.NGEnvelope;
import io.openraven.nightglow.api.Session;
import io.openraven.nightglow.core.fifos.FifoException;
import io.openraven.nightglow.core.fifos.FifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class OriginLayer implements Layer {

  private final static Logger LOGGER = LoggerFactory.getLogger(OriginLayer.class);

  private final Session session;
  private final Collection<EnumerationPlugin> plugins;
  private final FifoQueue queue;
  private final String name;

  public OriginLayer(String name, Session session, Collection<EnumerationPlugin> plugins, FifoQueue queue) {
    this.session = session;
    this.plugins = plugins;
    this.queue = queue;
    this.name = name;
  }

  @Override
  public void exec() throws FifoException {
    plugins.forEach(p -> {
      try {
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

  private void emit(NGEnvelope env) {
    try {
      queue.add(env);
    } catch (FifoException e) {
      LOGGER.warn("Emitter exception", e);
    }
  }

  @Override
  public LayerType getType() {
    return LayerType.ORIGIN;
  }
}
