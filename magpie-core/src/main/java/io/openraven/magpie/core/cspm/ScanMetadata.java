package io.openraven.magpie.core.cspm;

import java.time.Duration;
import java.util.Date;

public class ScanMetadata {
  private Date startDateTime;
  private Duration duration;

  public ScanMetadata(Date startDateTime, Duration duration) {
    this.startDateTime = startDateTime;
    this.duration = duration;
  }

  public Date getStartDateTime() {
    return startDateTime;
  }

  public Duration getDuration() {
    return duration;
  }
}
