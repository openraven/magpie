package io.openraven.magpie.cli;

import java.time.Duration;

public abstract class AbstractMain {

  protected static String humanReadableFormat(Duration duration) {
    return duration.toString()
      .substring(2)
      .replaceAll("(\\d[HMS])(?!$)", "$1 ")
      .toLowerCase();
  }

}
