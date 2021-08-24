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
package io.openraven.magpie.core.cspm.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.openraven.magpie.api.MagpiePlugin;
import io.openraven.magpie.api.PolicyOutputPlugin;
import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.core.cspm.analysis.ScanMetadata;
import io.openraven.magpie.core.cspm.analysis.ScanResults;
import io.openraven.magpie.core.plugins.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public class CspmFacadeImpl implements CspmFacade {

  private static final Logger LOGGER = LoggerFactory.getLogger(CspmFacadeImpl.class);
  private static final List<Class<? extends MagpiePlugin>> OUTPUT_PLUGIN_CLASSES = List.of(PolicyOutputPlugin.class);

  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Override
  public void analyze(MagpieConfig config) {
    final var start = Instant.now();

    var policyAcquisitionService = new PolicyAcquisitionServiceImpl();
    policyAcquisitionService.init(config);
    var policies = policyAcquisitionService.loadPolicies();

    var analyzerService = new PolicyAnalyzerServiceImpl();
    analyzerService.init(config);
    try {
      ScanResults scanResults = analyzerService.analyze(policies);
      var scanDuration = Duration.between(start, Instant.now());

      ScanMetadata scanMetadata = new ScanMetadata(Date.from(start), scanDuration);
      scanResults.setScanMetadata(scanMetadata);

      PluginManager policyPluginManager = new PluginManager(config);
      policyPluginManager.loadPlugins(OUTPUT_PLUGIN_CLASSES);

      List<MagpiePlugin<?>> outputPlugins = policyPluginManager.byType(PolicyOutputPlugin.class);

      // invoke output plugins
      outputPlugins.forEach(outputPlugin -> {
        LOGGER.debug("Executing output plugin : {}", outputPlugin.getClass());
        ((PolicyOutputPlugin<?>) outputPlugin).generateReport(mapper.valueToTree(scanResults));
      });
      // release all resources
      outputPlugins.forEach(MagpiePlugin::shutdown);

    } catch (Exception e) {
      LOGGER.error("Analyze error: {}", e.getMessage(), e);
    }

    LOGGER.info("Policy analysis completed in {}", humanReadableFormat(Duration.between(start, Instant.now())));
  }

  private static String humanReadableFormat(Duration duration) {
    return duration.toString()
      .substring(2)
      .replaceAll("(\\d[HMS])(?!$)", "$1 ")
      .toLowerCase();
  }
}
