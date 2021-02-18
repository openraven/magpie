package io.openraven.nightglow.core;

import java.util.HashMap;
import java.util.Map;

public class NightglowConfig {

  private Map<String, PluginConfig<?>> plugins = new HashMap<>();


  public Map<String, PluginConfig<?>> getPlugins() {
    return plugins;
  }

  public void setPlugins(Map<String, PluginConfig<?>> plugins) {
    this.plugins = plugins;
  }
}
