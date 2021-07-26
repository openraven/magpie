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
import io.openraven.magpie.core.dmap.client.DMapMLClientImpl;
import io.openraven.magpie.core.dmap.service.DMapAssetServiceImpl;
import io.openraven.magpie.core.dmap.service.DMapLambdaService;
import io.openraven.magpie.core.dmap.service.DMapLambdaServiceImpl;
import io.openraven.magpie.core.dmap.service.DMapReportServiceImpl;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;

public class DMap {

  private static final Logger LOGGER = LoggerFactory.getLogger(DMap.class);
  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
  private static final String DEFAULT_CONFIG_FILE = "config.yaml";
  public static final int DEFAULT_WORKERS_COUNT = 5;

  public static void main(String[] args) throws IOException, ParseException {

    var cmd = parseDMapOptions(args);

    var config = getConfig(cmd);
    var dMapAssetService = new DMapAssetServiceImpl(config);
    var vpcGroups = dMapAssetService.groupScanTargets();

    var workers = getWorkersCount(cmd);
    var objectMapper = new ObjectMapper();
    var dmapMLClient = new DMapMLClientImpl(objectMapper, config);
    var dMapLambdaService = new DMapLambdaServiceImpl(dmapMLClient, objectMapper, workers);
    Runtime.getRuntime().addShutdownHook(new CleanupDmapLambdaResourcesHook(dMapLambdaService));

    var dMapScanResult = dMapLambdaService.startDMapScan(vpcGroups);

    var dMapReportService = new DMapReportServiceImpl();
    dMapReportService.generateReport(dMapScanResult);

  }

  private static CommandLine parseDMapOptions(String[] args) throws ParseException {
    final var options = new Options();
    options.addOption(new Option("f", "configfile", true, "Config file location (defaults to " + DEFAULT_CONFIG_FILE + ")"));
    options.addOption(new Option("w", "workers", true, "Execution parallelism. Default workers: " + DEFAULT_WORKERS_COUNT + ")"));

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

    MagpieConfig config = null;

    try (var is = new FileInputStream((configFile))) {
      LOGGER.info("DMap. Classpath={}", System.getProperties().get("java.class.path"));
      config = ConfigUtils.merge(MAPPER.readValue(is, MagpieConfig.class), System.getenv());
    }
    return config;
  }

  private static int getWorkersCount(CommandLine cmd) {
    int workers = Optional.ofNullable(cmd.getOptionValue("w"))
      .map(Integer::valueOf)
      .orElse(DEFAULT_WORKERS_COUNT);
    LOGGER.info("DMap will be executed within {} threads. Use -w arg to change this parameter", workers);
    return workers;
  }

  private static class CleanupDmapLambdaResourcesHook extends Thread {

    private final DMapLambdaService dMapLambdaService;

    CleanupDmapLambdaResourcesHook(DMapLambdaService dMapLambdaService) {
      this.dMapLambdaService = dMapLambdaService;
      this.setName("shutdownhook");
    }

    @Override
    public void run() {
      dMapLambdaService.cleanupCreatedResources();
    }
  }
}
