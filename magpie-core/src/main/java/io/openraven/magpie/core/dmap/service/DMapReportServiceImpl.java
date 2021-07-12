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
  private static final int PORT_COLUMN_WIDTH = 9;
  private static final int BEGINING_PORT_COLUMN_WIDTH = 50;
  private static final int PROBABILITY_COLUMN_WIDTH = 14;
  private static final int APP_COLUMN_WIDTH = 15;

  @Override
  public void generateReport(DMapScanResult dMapScanResult) {
    System.out.println(BOLD_SET + "DMap Summary:" + BOLD_RESET);
    System.out.printf("%-30s%-40s\n", "DMap start time", dMapScanResult.getStartDateTime().toString());
    System.out.printf("%-30s%-40s\n\n", "DMap duration", humanReadableFormat(dMapScanResult.getDuration()));

    System.out.printf(BOLD_SET + "%-" + SERVICE_COLUMN_WIDTH + "s%-" + IP_COLUMN_WIDTH + "s%-" + PORT_COLUMN_WIDTH + "s%-" + PROBABILITY_COLUMN_WIDTH + "s%-" + APP_COLUMN_WIDTH + "s\n" + BOLD_RESET,
      "Resource Id", "IP Address", "Port", "Probability", "App");

    dMapScanResult.getFingerprintAnalyses().forEach(fingerprintAnalysis -> {

      System.out.printf("%-" + SERVICE_COLUMN_WIDTH + "s%-" + IP_COLUMN_WIDTH + "s",
        fingerprintAnalysis.getResourceId(), fingerprintAnalysis.getAddress());

      if(fingerprintAnalysis.getPredictionsByPort().isEmpty()) {
        System.out.printf("%-"+ PORT_COLUMN_WIDTH + "s%-" + PROBABILITY_COLUMN_WIDTH + "s%-" + APP_COLUMN_WIDTH + "s\n",
          "--", "--", "No available ports found for analysis");
      } else {
        System.out.println("");
      }

      new TreeMap<>(fingerprintAnalysis.getPredictionsByPort()).forEach((port, probabilities) -> {
        findTop(probabilities).ifPresentOrElse(
          app -> System.out.printf("%-41s%-"+ PORT_COLUMN_WIDTH + "s%-" + PROBABILITY_COLUMN_WIDTH + ".2f" + "%-4s\n",
            "", port, app.getProbability(), app.getAppName()),
          () -> System.out.printf("%-41s%-"+ PORT_COLUMN_WIDTH + "s%-" + PROBABILITY_COLUMN_WIDTH + "s%-" + APP_COLUMN_WIDTH + "s\n",
            "", port, "--", "Low probability: unable to identify app running port"));
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
