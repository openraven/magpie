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
    System.out.println("Scan Summary:");
    System.out.printf("%-30s%-40s\n", "Scan start time", this.scanMetadata.getStartDateTime().toString());
    System.out.printf("%-30s%-40s\n", "Scan duration", humanReadableFormat(this.scanMetadata.getDuration()));
    System.out.printf("%-30s%-40d\n\n", "Total violations found", violations.size());

    System.out.println("Scan Per-policy Details:");
    policies.forEach(policy -> {
      System.out.printf("%-30s%-40s\n", "Policy name", policy.getPolicy().getName());
      var policyViolations = violations.stream().filter(violation -> violation.getPolicyId().equals(policy.getPolicy().getId())).collect(Collectors.toList());
      System.out.printf("%-30s%-40s\n", "No. of violations", policyViolations.size());
      System.out.printf("%-30s\n", "Violations");
      System.out.printf("%-2s%-50s%-50s%-50s\n", "", "Resource ID", "Rule GUID", "Rule name");
      policyViolations.forEach(policyViolation -> {
        Rule violatedRule = policy.getPolicy().getRules().stream().filter(rule -> rule.getId().equals(policyViolation.getRuleId())).findFirst().get();
        System.out.printf("%-2s%-50s%-50s%-50s\n", "", policyViolation.getAssetId(), violatedRule.getId(), violatedRule.getName().replace(System.lineSeparator(), ""));
      });
      System.out.printf("\n");
    });
  }
}
