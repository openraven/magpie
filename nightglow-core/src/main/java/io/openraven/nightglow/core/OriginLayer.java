package io.openraven.nightglow.core;

import io.openraven.nightglow.api.DiscoveryEnvelope;
import io.openraven.nightglow.api.EnumerationPlugin;
import io.openraven.nightglow.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class OriginLayer implements Layer {

  private final static Logger LOGGER = LoggerFactory.getLogger(OriginLayer.class);

  private final Session session;
  private final Collection<EnumerationPlugin<?>> plugins;
  private final FifoQueue queue;

  public OriginLayer(Session session, Collection<EnumerationPlugin<?>> plugins, FifoQueue queue) {
    this.session = session;
    this.plugins = plugins;
    this.queue = queue;
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

  private void emit(DiscoveryEnvelope env) {
    try {
      queue.add(env);
    } catch (FifoException e) {
      LOGGER.warn("Emitter exception", e);
    }
  }
}
