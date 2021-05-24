package io.openraven.magpie.core.cspm;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Severity {
  @JsonProperty("high")
  HIGH("High"),
  @JsonProperty("medium")
  MEDIUM("Medium"),
  @JsonProperty("low")
  LOW("Low");

  private final String title;

  Severity(String title) {
    this.title = title;
  }

  public String getTitle() {
    return title;
  }
}
