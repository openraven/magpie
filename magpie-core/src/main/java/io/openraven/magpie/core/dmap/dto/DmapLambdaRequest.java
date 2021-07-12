package io.openraven.magpie.core.dmap.dto;

import java.util.Map;

public class DmapLambdaRequest {
  Map<String, String> hosts;

  public DmapLambdaRequest(Map<String, String> hosts) {
    this.hosts = hosts;
  }

  public Map<String, String> getHosts() {
    return hosts;
  }

  public void setHosts(Map<String, String> hosts) {
    this.hosts = hosts;
  }

}
