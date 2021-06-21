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

package io.openraven.magpie.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.openraven.magpie.core.config.ConfigUtils;
import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.core.cspm.ScanMetadata;
import io.openraven.magpie.core.cspm.ScanResults;
import io.openraven.magpie.core.cspm.services.*;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public class Policy {
  private static final Logger LOGGER = LoggerFactory.getLogger(Policy.class);
  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
  private static final String DEFAULT_CONFIG_FILE = "config.yaml";

  public static String humanReadableFormat(Duration duration) {
    return duration.toString()
      .substring(2)
      .replaceAll("(\\d[HMS])(?!$)", "$1 ")
      .toLowerCase();
  }

  public static void main(String[] args) throws IOException, ParseException {
    final var start = Instant.now();

    final var options = new Options();
    options.addOption(new Option("f", "configfile", true, "Config file location (defaults to " + DEFAULT_CONFIG_FILE + ")"));

    final var parser = new DefaultParser();
    final var cmd = parser.parse(options, args);

    var configFile = cmd.getOptionValue("f");
    if (configFile == null) {
      configFile = DEFAULT_CONFIG_FILE;
    } else {
      LOGGER.info("Using config file {}", configFile);
    }

    try (var is = new FileInputStream((configFile))) {
      final var config = ConfigUtils.merge(MAPPER.readValue(is, MagpieConfig.class), System.getenv());
      LOGGER.info("Policy. Classpath={}", System.getProperties().get("java.class.path"));

      PolicyAcquisitionService policyAcquisitionService = new PolicyAcquisitionServiceImpl();
      policyAcquisitionService.init(config);
      var policies = policyAcquisitionService.loadPolicies();
      PolicyAnalyzerService analyzerService = new PolicyAnalyzerServiceImpl();
      analyzerService.init(config);
      Duration scanDuration;
      try {
        ScanResults scanResults = analyzerService.analyze(policies);
        scanDuration = Duration.between(start, Instant.now());
        ReportService reportService = new ReportServiceImpl(new ScanMetadata(Date.from(start), scanDuration));
        reportService.generateReport(scanResults);
      } catch (Exception e) {
        LOGGER.error("Analyze error: {}", e.getMessage());
      }
    }

    LOGGER.info("Policy analysis  completed in {}", humanReadableFormat(Duration.between(start, Instant.now())));
  }
}
