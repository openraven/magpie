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

package io.openraven.magpie.core.dmap.service;

import io.openraven.magpie.core.dmap.client.dto.AppProbability;
import io.openraven.magpie.core.dmap.dto.DMapScanResult;

import java.time.Duration;
import java.util.*;

public class DMapReportServiceImpl implements DMapReportService {

  private static final double PROBABILITY_THRESHOLD = 0.5;

  private static final String BOLD_SET = "\033[1m";
  private static final String BOLD_RESET = "\033[0m";

  private static final int SERVICE_COLUMN_WIDTH = 23;
  private static final int IP_COLUMN_WIDTH = 18;
  private static final int REGION_COLUMN_WIDTH = 13;
  private static final int PORT_COLUMN_WIDTH = 9;
  private static final int PROBABILITY_COLUMN_WIDTH = 14;
  private static final int APP_COLUMN_WIDTH = 15;

  @Override
  public void generateReport(DMapScanResult dMapScanResult) {
    System.out.println(BOLD_SET + "DMap Summary:" + BOLD_RESET);
    System.out.printf("%-30s%-40s\n", "DMap start time", dMapScanResult.getStartDateTime().toString());
    System.out.printf("%-30s%-40s\n\n", "DMap duration", humanReadableFormat(dMapScanResult.getDuration()));

    System.out.printf(BOLD_SET + "%-" + REGION_COLUMN_WIDTH + "s%-" + SERVICE_COLUMN_WIDTH + "s%-" + IP_COLUMN_WIDTH + "s%-" + PORT_COLUMN_WIDTH + "s%-" + PROBABILITY_COLUMN_WIDTH + "s%-" + APP_COLUMN_WIDTH + "s\n" + BOLD_RESET,
      "Region", "Resource Id", "IP Address", "Port", "Probability", "App");

    dMapScanResult.getFingerprintAnalyses().forEach(fingerprintAnalysis -> {

      System.out.printf("%-" + REGION_COLUMN_WIDTH + "s%-" + SERVICE_COLUMN_WIDTH + "s%-" + IP_COLUMN_WIDTH + "s",
        fingerprintAnalysis.getRegion(), fingerprintAnalysis.getResourceId(), fingerprintAnalysis.getAddress());

      if(fingerprintAnalysis.getPredictionsByPort().isEmpty()) {
        System.out.printf("%-"+ PORT_COLUMN_WIDTH + "s%-" + PROBABILITY_COLUMN_WIDTH + "s%-" + APP_COLUMN_WIDTH + "s\n",
          "--", "--", "No open ports");
      } else {
        System.out.println("");
      }

      new TreeMap<>(fingerprintAnalysis.getPredictionsByPort()).forEach((port, probabilities) -> {
        findTop(probabilities).ifPresent(
          app -> System.out.printf("%-54s%-"+ PORT_COLUMN_WIDTH + "s%-" + PROBABILITY_COLUMN_WIDTH + ".2f" + "%-4s\n",
            "", port, app.getProbability(), app.getAppName()));
      });
    });
  }

  private Optional<AppProbability> findTop(List<AppProbability> appProbabilityList) {
    return appProbabilityList
      .stream()
      .filter(appProbability -> appProbability.getProbability() > PROBABILITY_THRESHOLD)
      .sorted()
      .findFirst();
  }

  // TODO: refactor move under Utils
  private String humanReadableFormat(Duration duration) {
    return duration.toString()
      .substring(2)
      .replaceAll("(\\d[HMS])(?!$)", "$1 ")
      .toLowerCase();
  }
}
