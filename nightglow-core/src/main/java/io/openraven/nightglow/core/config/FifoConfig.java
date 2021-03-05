package io.openraven.nightglow.core.config;


import java.util.HashMap;
import java.util.Map;

public class FifoConfig {

  private boolean enabled = true;
  private String type = "local";
  private Map<String, Object> properties = new HashMap<>();


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

  public Map<String, Object> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }
}
