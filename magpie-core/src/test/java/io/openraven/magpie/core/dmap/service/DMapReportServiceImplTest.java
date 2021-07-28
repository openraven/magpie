package io.openraven.magpie.core.dmap.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.core.dmap.dto.DMapScanResult;
import io.openraven.magpie.core.dmap.dto.FingerprintAnalysis;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;


class DMapReportServiceImplTest {

  private ObjectMapper objectMapper = new ObjectMapper();
  private DMapReportService dMapReportService = new DMapReportServiceImpl();

  @Test // This test is to validate the output format for the report only. It doesn't assert anything
  void testReportGeneration() throws JsonProcessingException {
    List<FingerprintAnalysis> fingerprintAnalyses = objectMapper.readValue(getResourceAsString("/dmap/finger-print-analysis.json"), new TypeReference<List<FingerprintAnalysis>>() {
    });
    Duration duration = Duration.between(Instant.now().minusSeconds(595), Instant.now());
    DMapScanResult dMapScanResult = new DMapScanResult(fingerprintAnalyses, Date.from(Instant.now()), duration);

    dMapReportService.generateReport(dMapScanResult);
  }

  // TODO: Move to Utility class
  private static String getResourceAsString(String resourcePath) {
    return new Scanner(
      Objects.requireNonNull(DMapReportServiceImplTest.class.getResourceAsStream(resourcePath)),
      StandardCharsets.UTF_8)
      .useDelimiter("\\A").next();
  }
}
