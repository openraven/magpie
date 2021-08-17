package io.openraven.magpie.plugins.policy.output.impl;

import io.openraven.magpie.api.PolicyOutputPlugin;
import io.openraven.magpie.api.cspm.PolicyContext;
import io.openraven.magpie.api.cspm.Rule;
import io.openraven.magpie.api.cspm.ScanResults;
import io.openraven.magpie.api.cspm.ScanResults.IgnoredReason;
import io.openraven.magpie.api.cspm.Violation;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Map;

public class CsvReportServiceImpl implements PolicyOutputPlugin<Void> {

  public static final String ID = "magpie.policy.output.csv";

  @Override
  public void generateReport(ScanResults results) {

    try (CSVPrinter printer = new CSVPrinter(System.out, CSVFormat.DEFAULT)) {

      // Headers
      printer.printRecord(
        "Policy name",
        "Resource ID",
        "Rule file name",
        "Rule name",
        "Ignored Reason");

      // Records
      if (!results.getViolations().isEmpty() || !results.getIgnoredRules().isEmpty()) {
        for (PolicyContext policyContext : results.getPolicies()) {
          var policyViolations = results.getViolations().get(policyContext);
          var ignoredRules = results.getIgnoredRules().get(policyContext);

          if (policyViolations != null) {
            for (Violation policyViolation : policyViolations) {
              Rule violatedRule = policyContext.getPolicy().getRules().stream()
                .filter(rule -> rule.getId().equals(policyViolation.getRuleId())).findFirst().get();
              printer.printRecord(
                policyContext.getPolicy().getPolicyName(),
                policyViolation.getAssetId(),
                violatedRule.getFileName(),
                trimLineSeparator(violatedRule.getRuleName()),
                null);
            }
          }

          if (ignoredRules != null) {
            for (Map.Entry<Rule, IgnoredReason> entry : ignoredRules.entrySet()) {
              Rule ignoredRule = entry.getKey();
              IgnoredReason reason = entry.getValue();
              printer.printRecord(
                policyContext.getPolicy().getPolicyName(),
                null,
                ignoredRule.getFileName(),
                trimLineSeparator(ignoredRule.getRuleName()),
                reason.getReason());
            }
          }
        }
      }

    } catch (IOException e) {
      throw new RuntimeException("Unable to serialize policy analysis results to CSV format", e);
    }
  }

  private String trimLineSeparator(String line) {
    return line.replace(System.lineSeparator(), "").replace("\"", "");
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public void init(Void unused, Logger logger) {
    // Nothing here yet
  }

  @Override
  public Class<Void> configType() {
    return null;
  }
}
