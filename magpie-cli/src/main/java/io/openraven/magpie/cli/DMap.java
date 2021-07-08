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
import io.openraven.magpie.core.cspm.services.DMapService;
import io.openraven.magpie.core.cspm.VpcConfig;
import io.openraven.magpie.core.cspm.services.DMapServiceImpl;
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
import java.util.List;
import java.util.Map;

public class DMap {

  private static final Logger LOGGER = LoggerFactory.getLogger(DMap.class);
  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
  private static final String DEFAULT_CONFIG_FILE = "config.yaml";

  public static void main(String[] args) throws IOException, ParseException {
    var config = getConfig(args);

    final var start = Instant.now();

    DMapService dMapService = new DMapServiceImpl(config);
    Map<VpcConfig, List<String>> vpcConfigListMap = dMapService.groupScanTargets();

    LOGGER.info("ScanTargets: {}", vpcConfigListMap);

    LOGGER.info("Policy analysis  completed in {}", humanReadableFormat(Duration.between(start, Instant.now())));
  }

  private static MagpieConfig getConfig(String[] args) throws ParseException, IOException {

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

    MagpieConfig config = null;

    try (var is = new FileInputStream((configFile))) {
      LOGGER.info("DMap. Classpath={}", System.getProperties().get("java.class.path"));
      config = ConfigUtils.merge(MAPPER.readValue(is, MagpieConfig.class), System.getenv());
    }
    return config;
  }

  public static String humanReadableFormat(Duration duration) {
    return duration.toString()
      .substring(2)
      .replaceAll("(\\d[HMS])(?!$)", "$1 ")
      .toLowerCase();
  }
}
