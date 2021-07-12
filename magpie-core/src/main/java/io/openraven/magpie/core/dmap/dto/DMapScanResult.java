package io.openraven.magpie.core.dmap.dto;

import java.time.Duration;
import java.util.Date;
import java.util.List;

public class DMapScanResult {

  private List<FingerprintAnalysis> fingerprintAnalyses;
  private Date startDateTime;
  private Duration duration;

  public DMapScanResult(List<FingerprintAnalysis> fingerprintAnalyses, Date startDateTime, Duration duration) {
    this.fingerprintAnalyses = fingerprintAnalyses;
    this.startDateTime = startDateTime;
    this.duration = duration;
  }

  public List<FingerprintAnalysis> getFingerprintAnalyses() {
    return fingerprintAnalyses;
  }

  public Date getStartDateTime() {
    return startDateTime;
  }

  public Duration getDuration() {
    return duration;
  }
}
