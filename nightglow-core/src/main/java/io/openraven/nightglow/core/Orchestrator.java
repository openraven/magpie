package io.openraven.nightglow.core;

import io.openraven.nightglow.api.Session;
import io.openraven.nightglow.core.config.NightglowConfig;
import io.openraven.nightglow.core.fifos.FifoManager;
import io.openraven.nightglow.core.layers.Layer;
import io.openraven.nightglow.core.layers.LayerManager;
import io.openraven.nightglow.core.layers.LayerType;
import io.openraven.nightglow.core.plugins.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

public class Orchestrator {

  // Allow layers to finish processing
  private static final long LAYER_GRACE_PERIOD = 3000L;
  private static final Logger LOGGER = LoggerFactory.getLogger(Orchestrator.class);
  private final NightglowConfig config;
  private final Session session;

  public Orchestrator(NightglowConfig config, Session session) {
    this.config = config;
    this.session = session;
  }

  public void scan() {
    final var fifoManager = new FifoManager(config);
    final var pluginManager = new PluginManager(config);
    final var layerManager = new LayerManager(session, config, fifoManager, pluginManager);

    final var layers = layerManager.getLayers();
    final var executors = Executors.newFixedThreadPool(layers.size(), r -> {
      Thread t = Executors.defaultThreadFactory().newThread(r);
      t.setDaemon(true);
      return t;
    });

    final var layerFutures = new ArrayList<Future<LayerType>>(layers.size());


    final var originLayers = layers.values().stream().filter(l -> l.getType() == LayerType.ORIGIN).collect(Collectors.toSet());
    final var otherLayers = layers.values().stream().filter(l -> !originLayers.contains(l)).collect(Collectors.toSet());


    var callables = new ArrayList<LayerCallable>();

    final var originFutures = originLayers.stream()
      .map(layer -> {
        var c = new LayerCallable(layer, false);
        callables.add(c);
        return executors.submit(c);
      })
      .collect(Collectors.toList());

    final var otherFutures = otherLayers.stream()
      .map(layer -> {
        var c = new LayerCallable(layer, true);
        callables.add(c);
        return executors.submit(c);
      })
      .collect(Collectors.toList());

    // Run indefinitely if no origin layers exist.  If one or more exist then wait for them all to complete.  In a
    // distributed setup the intermediate/terminal layers will always run as long-running streaming service, while
    // origin (discovery) layers may come and go.
    //
    // Intermediate and terminal layers never return unless there's an error, so we can wait on these with get() just
    // as we would for the possibly finite origin layer.
    var futures = originFutures.isEmpty() ? otherFutures : originFutures;
    futures.forEach(f -> {
      try {
        f.get();
      } catch (ExecutionException | InterruptedException ex) {
        LOGGER.error("Layer execution error", ex);
        System.exit(1);
      }
    });

    try {
      Thread.sleep(LAYER_GRACE_PERIOD);
    } catch (InterruptedException ex) {
      LOGGER.error("Grace period interrupted", ex);
    }

    LOGGER.debug(("Shutting down layers"));
    // Shut down all layers
    callables.forEach(c -> c.shutdown());
  }

  private class LayerCallable implements Callable<LayerType> {

    private final Layer layer;
    private volatile boolean repeat;

    public LayerCallable(Layer layer, Boolean repeat) {
      this.layer = layer;
      this.repeat = repeat;
    }

    @Override
    public LayerType call() throws Exception {
      do {
        try {
          layer.exec();
          Thread.sleep(100L);
        } catch (InterruptedException ex) {
          LOGGER.warn("Layer exec wait interrupted for {}", layer.getName(), ex);
        }
      } while (repeat);

      return layer.getType();
    }

    public void shutdown() {
      repeat = false;
    }
  }

}
