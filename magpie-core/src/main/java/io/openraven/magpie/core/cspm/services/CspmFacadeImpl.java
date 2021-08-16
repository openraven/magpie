package io.openraven.magpie.core.cspm.services;

import io.openraven.magpie.api.PolicyOutputPlugin;
import io.openraven.magpie.api.cspm.ScanMetadata;
import io.openraven.magpie.api.cspm.ScanResults;
import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.core.cspm.plugin.PolicyPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public class CspmFacadeImpl implements CspmFacade {

  private static final Logger LOGGER = LoggerFactory.getLogger(CspmFacadeImpl.class);

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

      PolicyPluginManager policyPluginManager = new PolicyPluginManager(config);

      policyPluginManager.byType(PolicyOutputPlugin.class)
        .stream()
        .map(p -> (PolicyOutputPlugin) p)
        .forEach(p -> p.generateReport(scanResults));


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
