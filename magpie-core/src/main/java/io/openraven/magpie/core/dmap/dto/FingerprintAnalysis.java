package io.openraven.magpie.core.dmap.dto;

import io.openraven.magpie.core.dmap.client.dto.AppProbability;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FingerprintAnalysis {
  private String resourceId;
  private String region;
  private String address;
  private Map<Integer, List<AppProbability>> predictionsByPort = new HashMap<>(); // Default empty predictions

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  public String getResourceId() {
    return resourceId;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getAddress() {
    return address;
  }

  public Map<Integer, List<AppProbability>> getPredictionsByPort() {
    return predictionsByPort;
  }

  public void setPredictionsByPort(Map<Integer, List<AppProbability>> predictionsByPort) {
    this.predictionsByPort = predictionsByPort;
  }

  @Override
  public String toString() {
    return "FingerprintAnalysis{" +
      "resourceId='" + resourceId + '\'' +
      ", address='" + address + '\'' +
      ", predictionsByPort=" + predictionsByPort +
      '}';
  }
}
