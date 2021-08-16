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
import io.openraven.magpie.core.cspm.services.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;

public class Policy {
  private static final Logger LOGGER = LoggerFactory.getLogger(Policy.class);
  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
  private static final String DEFAULT_CONFIG_FILE = "config.yaml";

  public static void main(String[] args) throws IOException, ParseException {

    final var cmd = parsePolicyOptions(args);
    final var config = getConfig(cmd);

    CspmFacade cspmFacade = new CspmFacadeImpl();
    cspmFacade.analyze(config);
  }

  private static CommandLine parsePolicyOptions(String[] args) throws ParseException {
    final var options = new Options();
    options.addOption(new Option("f", "configfile", true, "Config file location (defaults to " + DEFAULT_CONFIG_FILE + ")"));

    final var parser = new DefaultParser();
    return parser.parse(options, args);
  }

  private static MagpieConfig getConfig(CommandLine cmd) throws IOException {
    var configFile = cmd.getOptionValue("f");
    if (configFile == null) {
      configFile = DEFAULT_CONFIG_FILE;
    } else {
      LOGGER.info("Using config file {}", configFile);
    }

    try (var is = new FileInputStream((configFile))) {
      LOGGER.info("Policy. Classpath={}", System.getProperties().get("java.class.path"));
      return ConfigUtils.merge(MAPPER.readValue(is, MagpieConfig.class), System.getenv());
    }
  }
}
