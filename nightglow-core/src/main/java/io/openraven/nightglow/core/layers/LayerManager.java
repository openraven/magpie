package io.openraven.nightglow.core.layers;

import io.openraven.nightglow.core.config.NightglowConfig;
import io.openraven.nightglow.core.fifos.FifoManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class LayerManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(LayerManager.class);

  private final NightglowConfig config;
  private final Map<String, Layer> layers = new HashMap<>();
  public LayerManager(NightglowConfig config, FifoManager fifoManager) {
    this.config = config;
  }


  private void buildLayers(FifoManager fifoManager) {
    config.getLayers().forEach((name, layerConfig) -> {

      LOGGER.debug("Built layer {}", name);
    });
  }
}
