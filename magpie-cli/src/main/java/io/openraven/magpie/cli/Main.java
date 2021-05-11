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
import io.openraven.magpie.api.Session;
import io.openraven.magpie.core.Orchestrator;
import io.openraven.magpie.core.config.ConfigUtils;
import io.openraven.magpie.core.config.MagpieConfig;
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

public class Main {

  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
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
    final var cmd = parser.parse( options, args);

    var configFile = cmd.getOptionValue("f");
    if (configFile == null) {
      configFile = DEFAULT_CONFIG_FILE;
    }

    try(var is = new FileInputStream((configFile))) {
      final var config = ConfigUtils.merge(MAPPER.readValue(is, MagpieConfig.class), System.getenv());
      LOGGER.info("OSS Discovery. Classpath={}", System.getProperties().get("java.class.path"));
      new Orchestrator(config, new Session()).scan();
    }
    LOGGER.info("Discovery completed in {}", humanReadableFormat(Duration.between(start, Instant.now())));
  }
}
