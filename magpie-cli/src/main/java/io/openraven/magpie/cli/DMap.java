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
import io.openraven.magpie.core.dmap.DMapExecutionContext;
import io.openraven.magpie.core.dmap.model.EC2Target;
import io.openraven.magpie.core.dmap.dto.DMapScanResult;
import io.openraven.magpie.core.dmap.model.VpcConfig;
import io.openraven.magpie.core.dmap.service.DMapAssetService;
import io.openraven.magpie.core.dmap.service.DMapAssetServiceImpl;
import io.openraven.magpie.core.dmap.service.DMapLambdaService;
import io.openraven.magpie.core.dmap.service.DMapLambdaServiceImpl;
import io.openraven.magpie.core.dmap.service.DMapReportService;
import io.openraven.magpie.core.dmap.service.DMapReportServiceImpl;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;

public class DMap {

  private static final Logger LOGGER = LoggerFactory.getLogger(DMap.class);
  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
  private static final String DEFAULT_CONFIG_FILE = "config.yaml";
  public static final int DEFAULT_WORKERS_COUNT = 5;

  public static void main(String[] args) throws IOException, ParseException {

    var config = getConfig(args);
    var dMapAssetService = new DMapAssetServiceImpl(config);
    var vpcConfigListMap = dMapAssetService.groupScanTargets();

    var workers = getWorkersCount(args);
    var dMapLambdaService = new DMapLambdaServiceImpl(workers);
    var dMapExecutionContext = new DMapExecutionContext();
    Runtime.getRuntime().addShutdownHook(new CleanupDmapLambdaResourcesHook(dMapLambdaService, dMapExecutionContext));
    var dMapScanResult = dMapLambdaService.startDMapScan(vpcConfigListMap, dMapExecutionContext);

    var dMapReportService = new DMapReportServiceImpl();
    dMapReportService.generateReport(dMapScanResult);

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

  private static int getWorkersCount(String[] args) throws ParseException {
    final var options = new Options();
    options.addOption(new Option("w", "workers", true, "Execution parallelism. Default workers: " + DEFAULT_WORKERS_COUNT + ")"));

    final var parser = new DefaultParser();
    final var cmd = parser.parse(options, args);

    Integer workers = Integer.getInteger(cmd.getOptionValue("w"), DEFAULT_WORKERS_COUNT);
    LOGGER.info("DMap will be executed within {} threads. Use -w arg to change this parameter", workers);
    return workers;
  }

  private static class CleanupDmapLambdaResourcesHook extends Thread {

    private final DMapLambdaService dMapLambdaService;
    private final DMapExecutionContext dMapExecutionContext;

    CleanupDmapLambdaResourcesHook(DMapLambdaService dMapLambdaService, DMapExecutionContext dMapExecutionContext) {
      this.dMapLambdaService = dMapLambdaService;
      this.dMapExecutionContext = dMapExecutionContext;
      this.setName("shutdownhook");
    }

    @Override
    public void run() {
      dMapLambdaService.cleanupCreatedResources(dMapExecutionContext);
    }
  }
}
