package io.openraven.magpie.core.cspm.services;

import io.openraven.magpie.core.cspm.Rule;
import io.openraven.magpie.core.cspm.ScanMetadata;
import io.openraven.magpie.core.cspm.ScanResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Function;

public class ReportServiceImpl implements ReportService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReportServiceImpl.class);

  private final ScanMetadata scanMetadata;

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
  public void generateReport(ScanResults results) {
    final String BOLD_SET = "\033[1m";
    final String BOLD_RESET = "\033[0m";
    final int COLUMN_WIDTH = 60;
    final int GUID_COLUMN_WIDTH = 50;

    final Function<String, String> trimRuleName = (ruleName) -> {
      String trimmedName = ruleName.replace(System.lineSeparator(), "");
      trimmedName = trimmedName.length() >= COLUMN_WIDTH ? trimmedName.substring(0, COLUMN_WIDTH - "...".length() - 1) + "..." : trimmedName;
      return trimmedName;
    };


    System.out.println(BOLD_SET + "Scan Summary:" + BOLD_RESET);
    System.out.printf("%-30s%-40s\n", "Scan start time", this.scanMetadata.getStartDateTime().toString());
    System.out.printf("%-30s%-40s\n", "Scan duration", humanReadableFormat(this.scanMetadata.getDuration()));
    System.out.printf("%-30s%-40d\n\n", "Total violations found", results.getNumOfViolations());

    if (!results.getIgnoredPolicies().isEmpty()) {
      System.out.println(BOLD_SET + "Disabled policies:" + BOLD_RESET);
      System.out.printf(BOLD_SET + "%-2s%-" + GUID_COLUMN_WIDTH + "s%-" + COLUMN_WIDTH + "s\n" + BOLD_RESET, "", "Policy GUID", "Policy name");
      results.getIgnoredPolicies().forEach((policy, reason) -> {
        System.out.printf("%-2s%-" + GUID_COLUMN_WIDTH + "s%-" + COLUMN_WIDTH + "s\n", "", policy.getPolicy().getId(), policy.getPolicy().getName());
      });
      System.out.printf("\n");
    }

    if (!results.getViolations().isEmpty()) {
      System.out.println(BOLD_SET + "Scan Per-policy Details:" + BOLD_RESET);

      results.getViolations().forEach((policy, policyViolations) -> {
        System.out.printf("%-30s%-40s\n", "Policy name", policy.getPolicy().getName());
        System.out.printf("%-30s%-40s\n", "Policy status", policy.getPolicy().isEnabled() ? "Enabled" : "Disabled");
        System.out.printf("%-30s%-40s\n", "No. of violations", policyViolations.size());

        var ignoredRules = results.getIgnoredRules().get(policy);
        if (!ignoredRules.isEmpty()) {
          System.out.printf("%-30s\n", "Ignored rules");
          System.out.printf(BOLD_SET + "%-2s%-" + COLUMN_WIDTH + "s%-" + GUID_COLUMN_WIDTH + "s%-" + COLUMN_WIDTH + "s\n" + BOLD_RESET, "", "Rule name", "Rule GUID", "Reason");
          ignoredRules.forEach((rule, reason) -> {
            System.out.printf("%-2s%-" + COLUMN_WIDTH + "s%-" + GUID_COLUMN_WIDTH + "s%-" + COLUMN_WIDTH + "s\n", "", trimRuleName.apply(rule.getName()), rule.getId(), reason);
          });
          System.out.printf("\n");
        }

        System.out.printf("%-30s\n", "Violations");
        System.out.printf(BOLD_SET + "%-2s%-" + COLUMN_WIDTH + "s%-" + GUID_COLUMN_WIDTH + "s%-" + COLUMN_WIDTH + "s\n" + BOLD_RESET, "", "Resource ID", "Rule GUID", "Rule name");
        policyViolations.forEach(policyViolation -> {
          Rule violatedRule = policy.getPolicy().getRules().stream().filter(rule -> rule.getId().equals(policyViolation.getRuleId())).findFirst().get();
          String resourceID = policyViolation.getAssetId().length() >= COLUMN_WIDTH ?
            "..." + policyViolation.getAssetId().substring(policyViolation.getAssetId().length() - COLUMN_WIDTH + "...".length() + 2) : policyViolation.getAssetId();
          System.out.printf("%-2s%-" + COLUMN_WIDTH + "s%-" + GUID_COLUMN_WIDTH + "s%-" + COLUMN_WIDTH + "s\n", "", resourceID, violatedRule.getId(), trimRuleName.apply(violatedRule.getName()));
        });
        System.out.printf("\n");
      });
    }
  }
}
