package io.openraven.magpie.core.cspm.services;

import io.openraven.magpie.core.cspm.Rule;
import io.openraven.magpie.core.cspm.ScanMetadata;
import io.openraven.magpie.core.cspm.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class ReportServiceImpl implements ReportService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReportServiceImpl.class);

  private ScanMetadata scanMetadata;

  public ReportServiceImpl(ScanMetadata scanMetadata) {
    this.scanMetadata = scanMetadata;
  }

  private String humanReadableFormat(Duration duration) {
    return duration.toString()
      .substring(2)
      .replaceAll("(\\d[HMS])(?!$)", "$1 ")
      .toLowerCase();
  }

  @Override
  public void generateReport(List<PolicyContext> policies, List<Violation> violations) {
    final String BOLD_SET = "\033[1m";
    final String BOLD_RESET = "\033[0m";
    final int COLUMN_WIDTH = 60;
    final int GUID_COLUMN_WIDTH = 50;

    System.out.println(BOLD_SET + "Scan Summary:" + BOLD_RESET);
    System.out.printf("%-30s%-40s\n", "Scan start time", this.scanMetadata.getStartDateTime().toString());
    System.out.printf("%-30s%-40s\n", "Scan duration", humanReadableFormat(this.scanMetadata.getDuration()));
    System.out.printf("%-30s%-40d\n\n", "Total violations found", violations.size());

    System.out.println(BOLD_SET + "Scan Per-policy Details:" + BOLD_RESET);
    policies.forEach(policy -> {
      System.out.printf("%-30s%-40s\n", "Policy name", policy.getPolicy().getName());
      var policyViolations = violations.stream().filter(violation -> violation.getPolicyId().equals(policy.getPolicy().getId())).collect(Collectors.toList());
      System.out.printf("%-30s%-40s\n", "No. of violations", policyViolations.size());
      System.out.printf("%-30s\n", "Violations");
      System.out.printf(BOLD_SET + "%-2s%-" + COLUMN_WIDTH + "s%-" + GUID_COLUMN_WIDTH + "s%-" + COLUMN_WIDTH + "s\n" + BOLD_RESET, "", "Resource ID", "Rule GUID", "Rule name");
      policyViolations.forEach(policyViolation -> {
        Rule violatedRule = policy.getPolicy().getRules().stream().filter(rule -> rule.getId().equals(policyViolation.getRuleId())).findFirst().get();
        String resourceID = policyViolation.getAssetId().length() >= COLUMN_WIDTH ?
          "..." + policyViolation.getAssetId().substring(policyViolation.getAssetId().length() - COLUMN_WIDTH + "...".length() + 2) : policyViolation.getAssetId();
        String ruleName = violatedRule.getName().replace(System.lineSeparator(), "");
        ruleName = ruleName.length() >= COLUMN_WIDTH ? ruleName.substring(0, COLUMN_WIDTH - "...".length() - 1) + "..." : ruleName;
        System.out.printf("%-2s%-" + COLUMN_WIDTH + "s%-" + GUID_COLUMN_WIDTH + "s%-" + COLUMN_WIDTH + "s\n", "", resourceID, violatedRule.getId(), ruleName);
      });
      System.out.printf("\n");
    });
  }
}
