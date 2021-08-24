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
package io.openraven.magpie.plugins.policy.output.csv;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.openraven.magpie.api.PolicyOutputPlugin;
import io.openraven.magpie.plugins.policy.output.csv.analysis.IgnoredRule;
import io.openraven.magpie.plugins.policy.output.csv.analysis.ScanResults;
import io.openraven.magpie.plugins.policy.output.csv.analysis.Violation;
import io.openraven.magpie.plugins.policy.output.csv.exception.CsvOutputException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class CsvReportPlugin implements PolicyOutputPlugin<Void> {
  private static final Logger LOGGER = LoggerFactory.getLogger(CsvReportPlugin.class);

  private static final String ID = "magpie.policy.output.csv";

  private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

  private CSVPrinter printer;

  @Override
  public void generateReport(ObjectNode data) {
    ScanResults results = parseData(data);

    try {

      // Headers
      printer.printRecord(
        "Policy name",
        "Resource ID",
        "Rule file name",
        "Rule name",
        "Ignored Reason");

      // Records
      if (!results.getViolations().isEmpty() || !results.getIgnoredRules().isEmpty()) {

        // Violations
        for (Violation violation : results.getViolations()) {
          printer.printRecord(
            violation.getPolicy().getPolicyName(),
            violation.getAssetId(),
            violation.getRule().getFileName(),
            trimLineSeparator(violation.getRule().getRuleName()),
            null);
        }

        // Ignored rules
        for (IgnoredRule ignoredRule : results.getIgnoredRules()) {
          printer.printRecord(
            ignoredRule.getPolicy().getPolicyName(),
            null,
            ignoredRule.getRule().getFileName(),
            trimLineSeparator(ignoredRule.getRule().getRuleName()),
            ignoredRule.getIgnoredReason().getReason());
        }
      }

    } catch (IOException e) {
      throw new RuntimeException("Unable to serialize policy analysis results to CSV format", e);
    }
  }

  private String trimLineSeparator(String line) {
    return line.replace(System.lineSeparator(), "").replace("\"", "");
  }

  protected ScanResults parseData(ObjectNode data) {
    try {
      return MAPPER.treeToValue(data, ScanResults.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Unable to parse data for plugin: " + id(), e);
    }
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public void init(Void config, Logger logger) {
    try {
      printer = new CSVPrinter(System.out, CSVFormat.DEFAULT);
    } catch (IOException e) {
      throw new CsvOutputException("Unable to instantiate the CSVPrinter for system.out stream", e);
    }
  }

  @Override
  public void shutdown() {
    try {
      printer.close(true);
    } catch (IOException e) {
      throw new CsvOutputException("Unable to close the CSVPrinter", e);
    }
  }

  @Override
  public Class<Void> configType() {
    return null;
  }

}
