package io.openraven.magpie.core.cspm.services.report;

import io.openraven.magpie.core.cspm.Rule;
import io.openraven.magpie.core.cspm.ScanMetadata;
import io.openraven.magpie.core.cspm.ScanResults;
import io.openraven.magpie.core.cspm.ScanResults.IgnoredReason;
import io.openraven.magpie.core.cspm.Violation;
import io.openraven.magpie.core.cspm.services.PolicyContext;
import io.openraven.magpie.core.cspm.services.ReportService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class CsvReportService implements ReportService {

  private final ScanMetadata scanMetadata;

  public CsvReportService(ScanMetadata scanMetadata) {
    this.scanMetadata = scanMetadata;
  }

  @Override
  public void generateReport(ScanResults results) {

    try (CSVPrinter printer = new CSVPrinter(System.out, CSVFormat.DEFAULT)) {
      generateReportMeta(printer, results);
      generateDisabledPolicies(printer, results);
      generatePolicyAnalysis(printer, results);

    } catch (IOException e) {
      throw new RuntimeException("Unable to serialize policy analysis results to CSV format", e);
    }
  }

  private void generateReportMeta(CSVPrinter printer, ScanResults results) throws IOException {
    printer.printRecord("Scan Summary:");
    printer.printRecord("Scan start time", "Scan duration", "Total violations found");
    printer.printRecord(scanMetadata.getStartDateTime().toString(),
      humanReadableFormat(this.scanMetadata.getDuration()), results.getNumOfViolations());
    printer.println();
  }

  private void generateDisabledPolicies(CSVPrinter printer, ScanResults results) throws IOException {
    var disabledPolicies = results.getPolicies().stream().filter(policy -> !policy.getPolicy().isEnabled()).collect(Collectors.toList());
    if (!disabledPolicies.isEmpty()) {
      printer.printRecord("Disabled policies:");
      printer.printRecord("Policy GUID", "Policy name");
      for (PolicyContext policy : disabledPolicies) { // avoiding lambda for cleaner code due to required Excptn handling
        printer.printRecord(policy.getPolicy().getId(), policy.getPolicy().getPolicyName());
      }
      printer.println();
    }
  }

  private void generatePolicyAnalysis(CSVPrinter printer, ScanResults results) throws IOException {
    if (!results.getViolations().isEmpty() || !results.getIgnoredRules().isEmpty()) {
      printer.printRecord("Scan Per-policy Details:");
      for (PolicyContext policy : results.getPolicies()) {
        var policyViolations = results.getViolations().get(policy);
        var ignoredRules = results.getIgnoredRules().get(policy);

        printer.printRecord("Policy name", "No. of violations");
        printer.printRecord(policy.getPolicy().getPolicyName(), policyViolations == null ? 0 : policyViolations.size());

        generateViolations(policyViolations, policy, printer);
        generateIgnoredRules(ignoredRules, printer);
        printer.println();
      }
    }
  }

  private void generateViolations(List<Violation> policyViolations,
                                  PolicyContext policyContext,
                                  CSVPrinter printer) throws IOException {
    if (policyViolations != null) {
      printer.printRecord("Violations:");
      printer.printRecord("Resource ID", "Rule file name", "Rule name");
      for (Violation policyViolation : policyViolations) {
        Rule violatedRule = policyContext.getPolicy().getRules().stream()
          .filter(rule -> rule.getId().equals(policyViolation.getRuleId())).findFirst().get();
        printer.printRecord(
          policyViolation.getAssetId(),
          violatedRule.getFileName(),
          trimLineSeparator(violatedRule.getRuleName()));
      }
      printer.println();
    }
  }

  private void generateIgnoredRules(Map<Rule, IgnoredReason> ignoredRules,
                                    CSVPrinter printer) throws IOException {
    if (ignoredRules != null) {
      printer.printRecord("Ignored rules:");
      printer.printRecord( "Rule name", "Rule file name", "Reason");
      for (Entry<Rule, IgnoredReason> entry : ignoredRules.entrySet()) {
        Rule rule = entry.getKey();
        IgnoredReason reason = entry.getValue();
        printer.printRecord(
          trimLineSeparator(rule.getRuleName()),
          rule.getFileName(),
          reason.getReason());
      }
      printer.println();
    }
  }

  private String trimLineSeparator(String line) {
    return line.replace(System.lineSeparator(), "").replace("\"", "");
  }

  private String humanReadableFormat(Duration duration) {
    return duration.toString()
      .substring(2)
      .replaceAll("(\\d[HMS])(?!$)", "$1 ")
      .toLowerCase();
  }
}
