package io.openraven.magpie.core.dmap.dto;

import java.util.Map;

public class DMapFingerprints {
  private String id;
  private String address;
  private Map<Integer, Map<String, String>> signatures;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public Map<Integer, Map<String, String>> getSignatures() {
    return signatures;
  }

  public void setSignatures(Map<Integer, Map<String, String>> signatures) {
    this.signatures = signatures;
  }

  @Override
  public String toString() {
    return "DMapFingerprints{" +
      "id='" + id + '\'' +
      ", address='" + address + '\'' +
      ", signatures=" + signatures +
      '}';
  }
}
