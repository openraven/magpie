package io.openraven.magpie.core.dmap.dto;


public class LambdaDetails {

  private String region;
  private String functionName;

  public LambdaDetails(String region, String functionName) {
    this.region = region;
    this.functionName = functionName;
  }

  public String getRegion() {
    return region;
  }

  public String getFunctionName() {
    return functionName;
  }
}
