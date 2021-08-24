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
package io.openraven.magpie.plugins.policy.output.text;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.openraven.magpie.api.PolicyOutputPlugin;
import io.openraven.magpie.plugins.policy.output.text.analysis.IgnoredRule;
import io.openraven.magpie.plugins.policy.output.text.analysis.ScanResults;
import io.openraven.magpie.plugins.policy.output.text.analysis.Violation;
import io.openraven.magpie.plugins.policy.output.text.model.Policy;
import io.openraven.magpie.plugins.policy.output.text.model.Rule;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class TextReportPlugin implements PolicyOutputPlugin<Void> {

  private static final String ID = "magpie.policy.output.text";

  private static final String BOLD_SET = "\033[1m";
  private static final String BOLD_RESET = "\033[0m";
  private static final int COLUMN_WIDTH = 60;
  private static final int GUID_COLUMN_WIDTH = 50;
  private static final int FILE_NAME_COLUMN_WIDTH = 55;

  private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
  private static final BiFunction<String, Integer, String> TRIM_BY_COLUMN_FUNCTION = (value, columnWidth) -> {
    String trimmedValue = value.replace(System.lineSeparator(), "");
    trimmedValue = trimmedValue.length() >= columnWidth
      ? trimmedValue.substring(0, columnWidth - "...".length() - 1) + "..."
      : trimmedValue;
    return trimmedValue;
  };

  @Override
  public void generateReport(ObjectNode data) {
    ScanResults results = parseData(data);

    processMeta(results);
    processDisabledPolicies(results);

    if (!results.getViolations().isEmpty() || !results.getIgnoredRules().isEmpty()) {
      System.out.println(BOLD_SET + "Scan Per-policy Details:" + BOLD_RESET);

      Map<Policy, List<Violation>> violationMap = results.getViolations().stream()
        .collect(groupingBy(Violation::getPolicy, toList()));

      Map<Policy, List<IgnoredRule>> ignoredRulesMap = results.getIgnoredRules().stream()
        .collect(groupingBy(IgnoredRule::getPolicy));

      results.getPolicies().forEach(policy -> {

        Optional.ofNullable(violationMap.get(policy)).ifPresent(violations -> processViolations(policy, violations));
        Optional.ofNullable(ignoredRulesMap.get(policy)).ifPresent(this::processIgnoredRules);

        System.out.println("");
      });
    }
  }

  private void processIgnoredRules(List<IgnoredRule> ignoredRules) {
    System.out.printf("%-30s\n", "Ignored rules");
    System.out.printf(BOLD_SET + "%-2s%-" + COLUMN_WIDTH + "s%-" + FILE_NAME_COLUMN_WIDTH + "s%-" + COLUMN_WIDTH + "s\n" + BOLD_RESET,
      "", "Rule name", "Rule file name", "Reason");

    ignoredRules.forEach(ignoredRule -> {
      System.out.printf("%-2s%-" + COLUMN_WIDTH + "s%-" + FILE_NAME_COLUMN_WIDTH + "s%-" + COLUMN_WIDTH + "s\n",
        "", TRIM_BY_COLUMN_FUNCTION.apply(ignoredRule.getRule().getRuleName(), COLUMN_WIDTH),
        TRIM_BY_COLUMN_FUNCTION.apply(ignoredRule.getRule().getFileName(), FILE_NAME_COLUMN_WIDTH),
        ignoredRule.getIgnoredReason().getReason());
    });
  }

  private void processViolations(Policy policy, List<Violation> policyViolations) {
    System.out.printf("%-30s%-40s\n", "Policy name", policy.getPolicyName());
    System.out.printf("%-30s%-40s\n", "No. of violations", policyViolations.size());

    System.out.printf("%-30s\n", "Violations");
    System.out.printf(BOLD_SET + "%-2s%-" + COLUMN_WIDTH + "s%-" + FILE_NAME_COLUMN_WIDTH + "s%-" + COLUMN_WIDTH + "s\n" + BOLD_RESET,
      "", "Resource ID", "Rule file name", "Rule name");

    policyViolations.forEach(policyViolation -> {

      String resourceID = policyViolation.getAssetId().length() >= COLUMN_WIDTH
        ? "..." + policyViolation.getAssetId().substring(policyViolation.getAssetId().length() - COLUMN_WIDTH + "...".length() + 2)
        : policyViolation.getAssetId();

      Rule violatedRule = policyViolation.getRule();
      System.out.printf("%-2s%-" + COLUMN_WIDTH + "s%-" + FILE_NAME_COLUMN_WIDTH + "s%-" + COLUMN_WIDTH + "s\n",
        "", resourceID,
        TRIM_BY_COLUMN_FUNCTION.apply(violatedRule.getFileName(), FILE_NAME_COLUMN_WIDTH),
        TRIM_BY_COLUMN_FUNCTION.apply(violatedRule.getRuleName(), COLUMN_WIDTH));
    });
    System.out.println("");
  }

  private void processDisabledPolicies(ScanResults results) {
    // Disabled policies render
    var disabledPolicies = results.getPolicies().stream().filter(policy -> !policy.isEnabled()).collect(Collectors.toList());
    if (!disabledPolicies.isEmpty()) {
      System.out.println(BOLD_SET + "Disabled policies:" + BOLD_RESET);
      System.out.printf(BOLD_SET + "%-2s%-" + GUID_COLUMN_WIDTH + "s%-" + COLUMN_WIDTH + "s\n" + BOLD_RESET,
        "", "Policy GUID", "Policy name");
      disabledPolicies.forEach(policy -> {
        System.out.printf("%-2s%-" + GUID_COLUMN_WIDTH + "s%-" + COLUMN_WIDTH + "s\n",
          "", policy.getId(), policy.getPolicyName());
      });
      System.out.printf("\n");
    }
  }

  private void processMeta(ScanResults results) {
    System.out.println(BOLD_SET + "Scan Summary:" + BOLD_RESET);
    System.out.printf("%-30s%-40s\n", "Scan start time", results.getScanMetadata().getStartDateTime().toString());
    System.out.printf("%-30s%-40s\n", "Scan duration", humanReadableFormat(results.getScanMetadata().getDuration()));
    System.out.printf("%-30s%-40d\n\n", "Total violations found", results.getViolations().size());
  }

  private ScanResults parseData(ObjectNode data) {
    try {
      return MAPPER.treeToValue(data, ScanResults.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Unable to parse data for plugin: " + id(), e);
    }
  }

  private String humanReadableFormat(Duration duration) {
    return duration.toString()
      .substring(2)
      .replaceAll("(\\d[HMS])(?!$)", "$1 ")
      .toLowerCase();
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public void init(Void config, Logger logger) {

  }

  @Override
  public void shutdown() {
    PolicyOutputPlugin.super.shutdown();
  }

  @Override
  public Class<Void> configType() {
    return null;
  }

}
