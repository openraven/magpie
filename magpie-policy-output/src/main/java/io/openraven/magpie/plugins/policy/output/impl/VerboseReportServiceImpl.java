package io.openraven.magpie.plugins.policy.output.impl;

import io.openraven.magpie.api.PolicyOutputPlugin;
import io.openraven.magpie.api.cspm.Rule;
import io.openraven.magpie.api.cspm.ScanResults;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class VerboseReportServiceImpl implements PolicyOutputPlugin<Void> {

  private static final String ID = "magpie.policy.output.verbose";

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
    final int FILE_NAME_COLUMN_WIDTH = 55;

    final BiFunction<String, Integer, String> trimColumnValue = (value, columnWidth) -> {
      String trimmedValue = value.replace(System.lineSeparator(), "");
      trimmedValue = trimmedValue.length() >= columnWidth ? trimmedValue.substring(0, columnWidth - "...".length() - 1) + "..." : trimmedValue;
      return trimmedValue;
    };

    System.out.println(BOLD_SET + "Scan Summary:" + BOLD_RESET);
    System.out.printf("%-30s%-40s\n", "Scan start time", results.getScanMetadata().getStartDateTime().toString());
    System.out.printf("%-30s%-40s\n", "Scan duration", humanReadableFormat(results.getScanMetadata().getDuration()));
    System.out.printf("%-30s%-40d\n\n", "Total violations found", results.getNumOfViolations());

    var disabledPolicies = results.getPolicies().stream().filter(policy -> !policy.getPolicy().isEnabled()).collect(Collectors.toList());
    if (!disabledPolicies.isEmpty()) {
      System.out.println(BOLD_SET + "Disabled policies:" + BOLD_RESET);
      System.out.printf(BOLD_SET + "%-2s%-" + GUID_COLUMN_WIDTH + "s%-" + COLUMN_WIDTH + "s\n" + BOLD_RESET, "", "Policy GUID", "Policy name");
      disabledPolicies.forEach(policy -> {
        System.out.printf("%-2s%-" + GUID_COLUMN_WIDTH + "s%-" + COLUMN_WIDTH + "s\n", "", policy.getPolicy().getId(), policy.getPolicy().getPolicyName());
      });
      System.out.printf("\n");
    }

    if (!results.getViolations().isEmpty() || !results.getIgnoredRules().isEmpty()) {
      System.out.println(BOLD_SET + "Scan Per-policy Details:" + BOLD_RESET);

      results.getPolicies().forEach(policy -> {
        var policyViolations = results.getViolations().get(policy);
        var ignoredRules = results.getIgnoredRules().get(policy);

        System.out.printf("%-30s%-40s\n", "Policy name", policy.getPolicy().getPolicyName());
        System.out.printf("%-30s%-40s\n", "No. of violations", policyViolations == null ? 0 : policyViolations.size());

        if (policyViolations != null) {
          System.out.printf("%-30s\n", "Violations");
          System.out.printf(BOLD_SET + "%-2s%-" + COLUMN_WIDTH + "s%-" + FILE_NAME_COLUMN_WIDTH + "s%-" + COLUMN_WIDTH + "s\n" + BOLD_RESET, "", "Resource ID", "Rule file name", "Rule name");
          policyViolations.forEach(policyViolation -> {
            Rule violatedRule = policy.getPolicy().getRules().stream().filter(rule -> rule.getId().equals(policyViolation.getRuleId())).findFirst().get();
            String resourceID = policyViolation.getAssetId().length() >= COLUMN_WIDTH ?
              "..." + policyViolation.getAssetId().substring(policyViolation.getAssetId().length() - COLUMN_WIDTH + "...".length() + 2) : policyViolation.getAssetId();
            System.out.printf("%-2s%-" + COLUMN_WIDTH + "s%-" + FILE_NAME_COLUMN_WIDTH + "s%-" + COLUMN_WIDTH + "s\n", "", resourceID, trimColumnValue.apply(violatedRule.getFileName(), FILE_NAME_COLUMN_WIDTH), trimColumnValue.apply(violatedRule.getRuleName(), COLUMN_WIDTH));
          });
          System.out.printf("\n");
        }

        if (ignoredRules != null) {
          System.out.printf("%-30s\n", "Ignored rules");
          System.out.printf(BOLD_SET + "%-2s%-" + COLUMN_WIDTH + "s%-" + FILE_NAME_COLUMN_WIDTH + "s%-" + COLUMN_WIDTH + "s\n" + BOLD_RESET, "", "Rule name", "Rule file name", "Reason");
          ignoredRules.forEach((rule, reason) -> {
            System.out.printf("%-2s%-" + COLUMN_WIDTH + "s%-" + FILE_NAME_COLUMN_WIDTH + "s%-" + COLUMN_WIDTH + "s\n", "", trimColumnValue.apply(rule.getRuleName(), COLUMN_WIDTH), trimColumnValue.apply(rule.getFileName(), FILE_NAME_COLUMN_WIDTH), reason.getReason());
          });
        }

        System.out.printf("\n");
      });
    }
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public void init(Void unused, Logger logger) {
    // Nothing here
  }

  @Override
  public Class<Void> configType() {
    return null;
  }
}