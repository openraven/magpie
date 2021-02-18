package io.openraven.nightglow.core;

public class PluginConfig<T> {

  private boolean enabled = true;
  private T config;


  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public T getConfig() {
    return config;
  }

  public void setConfig(T config) {
    this.config = config;
  }
}
