package io.openraven.nightglow.core.config;

import java.util.HashMap;
import java.util.Map;

public class NightglowConfig {

  private Map<String, LayerConfig> layers = new HashMap<>();
  private Map<String, FifoConfig> fifos = new HashMap<>();
  private Map<String, PluginConfig> plugins = new HashMap<>();

  public Map<String, FifoConfig> getFifos() {
    return fifos;
  }

  public void setFifos(Map<String, FifoConfig> fifos) {
    this.fifos = fifos;
  }

  public Map<String, LayerConfig> getLayers() {
    return layers;
  }

  public void setLayers(Map<String, LayerConfig> layers) {
    this.layers = layers;
  }

  public Map<String, PluginConfig> getPlugins() {
    return plugins;
  }

  public void setPlugins(Map<String, PluginConfig> plugins) {
    this.plugins = plugins;
  }
}
