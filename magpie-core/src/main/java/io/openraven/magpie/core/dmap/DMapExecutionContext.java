package io.openraven.magpie.core.dmap;


public class DMapExecutionContext {

  private String region;
  private String lambdaName;

  public void setRegion(String region) {
    this.region = region;
  }

  public String getRegion() {
    return region;
  }

  public void setLambdaName(String lambdaName) {

    this.lambdaName = lambdaName;
  }

  public String getLambdaName() {
    return lambdaName;
  }

  public void clear() {
    this.region = null;
    this.lambdaName = null;
  }
}
