/*
 * Copyright 2021 Open Raven Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
