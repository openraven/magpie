package io.openraven.nightglow.core;

import io.openraven.nightglow.api.Session;
import io.openraven.nightglow.core.config.NightglowConfig;
import io.openraven.nightglow.core.fifos.FifoManager;
import io.openraven.nightglow.core.layers.Layer;
import io.openraven.nightglow.core.layers.LayerManager;
import io.openraven.nightglow.core.plugins.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Orchestrator {

  private class LayerCallable implements Callable<Void> {

    private final Layer layer;

    public LayerCallable(Layer layer) {
      this.layer = layer;
    }

    @Override
    public Void call() throws Exception {
      while(true) {
        try {
          layer.exec();
          Thread.sleep(50);
        } catch (InterruptedException ex) {
          LOGGER.info("Scheduled sleep failed, shutting down layer {}", layer.getName(), ex);
          break;
        }
      }
      return null;
    }
  }

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
    final var executors = Executors.newFixedThreadPool(layers.size());
    List<Future<Void>> layerFutures = new ArrayList<>(layers.size());
    layers.values().forEach(l -> layerFutures.add(executors.submit(new LayerCallable(l))));

    while(layerFutures.stream().noneMatch(Future::isDone)) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException ex) {
        LOGGER.info("Shutting down layers.");
        layerFutures.forEach(f -> f.cancel(true));
      }
    }

    layerFutures.stream().forEach(f -> f.cancel(false));


  }

}
