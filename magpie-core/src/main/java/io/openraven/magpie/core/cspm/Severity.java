package io.openraven.magpie.core.cspm;

public enum Severity {
  HIGH("High"),
  MEDIUM("Medium"),
  LOW("Low");

  private final String title;

  Severity(String title) {
    this.title = title;
  }

  public String getTitle() {
    return title;
  }
}
