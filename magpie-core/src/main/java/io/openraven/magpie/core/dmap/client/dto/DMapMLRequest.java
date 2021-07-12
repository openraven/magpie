package io.openraven.magpie.core.dmap.client.dto;


import java.util.Map;

public class DMapMLRequest {
  private Metadata metadata = new Metadata();
  private Map<String, String> signature;

  public Metadata getMetadata() {
    return metadata;
  }

  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }

  public Map<String, String> getSignature() {
    return signature;
  }

  public void setSignature(Map<String, String> signature) {
    this.signature = signature;
  }

  public static class Metadata {
    private String appName = ""; // TODO remove defults
    private String appVersion = "";
    private String src = "";

    public String getAppName() {
      return appName;
    }

    public void setAppName(String appName) {
      this.appName = appName;
    }

    public String getAppVersion() {
      return appVersion;
    }

    public void setAppVersion(String appVersion) {
      this.appVersion = appVersion;
    }

    public String getSrc() {
      return src;
    }

    public void setSrc(String src) {
      this.src = src;
    }
  }
}
