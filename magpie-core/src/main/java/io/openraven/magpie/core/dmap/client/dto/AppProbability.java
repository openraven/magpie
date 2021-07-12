package io.openraven.magpie.core.dmap.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class AppProbability implements Comparable<AppProbability> {

  @JsonAlias("app_name")
  private String appName;

  private double probability;

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public double getProbability() {
    return probability;
  }

  public void setProbability(double probability) {
    this.probability = probability;
  }

  @Override
  public int compareTo(AppProbability other) {
    if (this.probability > other.probability) {
      return 1;
    } else if (this.probability < other.probability) {
      return -1;
    }
    return 0;
  }

  @Override
  public String toString() {
    return "AppProbability{" +
      "appName='" + appName + '\'' +
      ", probability=" + probability +
      '}';
  }
}
