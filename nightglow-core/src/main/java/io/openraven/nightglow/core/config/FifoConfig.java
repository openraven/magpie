package io.openraven.nightglow.core.config;


public class FifoConfig {
  private boolean enabled = true;

  private String type = "local";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
