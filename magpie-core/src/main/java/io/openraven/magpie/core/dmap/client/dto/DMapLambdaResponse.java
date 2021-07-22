package io.openraven.magpie.core.dmap.client.dto;

import io.openraven.magpie.core.dmap.dto.DMapFingerprints;

import java.util.Map;

public class DMapLambdaResponse {

  private Map<String, DMapFingerprints> hosts;

  public Map<String, DMapFingerprints> getHosts() {
    return hosts;
  }

  public void setHosts(Map<String, DMapFingerprints> hosts) {
    this.hosts = hosts;
  }
}
