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

package io.openraven.magpie.core;

import io.openraven.magpie.api.Session;
import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.core.fifos.FifoManager;
import io.openraven.magpie.core.layers.Layer;
import io.openraven.magpie.core.layers.LayerManager;
import io.openraven.magpie.core.layers.LayerType;
import io.openraven.magpie.core.plugins.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Orchestrator {

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
        } catch (Exception ex) {
          LOGGER.warn("Layer exception", ex);
        }
      } while (repeat);

      return layer.getType();
    }

    public void shutdown() {
      layer.shutdown();
      repeat = false;
    }
  }

  // Allow layers to finish processing
  private static final long LAYER_GRACE_PERIOD = 3000L;
  private static final Logger LOGGER = LoggerFactory.getLogger(Orchestrator.class);
  private final MagpieConfig config;
  private final Session session;

  public Orchestrator(MagpieConfig config, Session session) {
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

    final var originLayers = layers.values().stream().filter(l -> l.getType() == LayerType.ORIGIN).collect(Collectors.toSet());
    final var otherLayers = layers.values().stream().filter(l -> !originLayers.contains(l)).collect(Collectors.toSet());

    var callables = new ArrayList<LayerCallable>();

    final var originFutures = originLayers.stream()
      .map(layer -> {
        var c = new LayerCallable(layer, false);
        callables.add(c);
        LOGGER.trace("Submitting callable {}", c.layer.getName());
        return executors.submit(c);
      })
      .collect(Collectors.toList());

    final var otherFutures = otherLayers.stream()
      .map(layer -> {
        var c = new LayerCallable(layer, true);
        callables.add(c);
        LOGGER.trace("Submitting callable {}", c.layer.getName());
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
        LOGGER.trace("Got {}", f.get());
      } catch (ExecutionException | InterruptedException ex) {
        LOGGER.error("Layer execution error", ex);
        System.exit(1);
      }
    });

    try {
      LOGGER.debug("Entering grace period");
      Thread.sleep(LAYER_GRACE_PERIOD);
      LOGGER.debug("Exited grade period");
    } catch (InterruptedException ex) {
      LOGGER.error("Grace period interrupted", ex);
    }

    LOGGER.debug(("Shutting down layers"));
    // Shut down all layers
    callables.forEach(LayerCallable::shutdown);
  }
}
