package io.openraven.nightglow.core.config;

import java.util.ArrayList;
import java.util.List;

public class LayerConfig {
  private boolean enabled = true;
  private String type;

  private String queue;
  private String dequeue;

  private List<String> plugins = new ArrayList<>();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getQueue() {
    return queue;
  }

  public void setQueue(String queue) {
    this.queue = queue;
  }

  public String getDequeue() {
    return dequeue;
  }

  public void setDequeue(String dequeue) {
    this.dequeue = dequeue;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public List<String> getPlugins() {
    return plugins;
  }

  public void setPlugins(List<String> plugins) {
    this.plugins = plugins;
  }
}
