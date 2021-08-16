package io.openraven.magpie.core.cspm.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.cspm.*;
import io.openraven.magpie.core.config.ConfigException;
import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.plugins.persist.PersistConfig;
import io.openraven.magpie.plugins.persist.PersistPlugin;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PolicyAnalyzerServiceImpl implements PolicyAnalyzerService {
  private static final Logger LOGGER = LoggerFactory.getLogger(PolicyAnalyzerServiceImpl.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String EXIST_ASSETS_PER_CLOUD =
    "SELECT EXISTS(SELECT 1 FROM assets WHERE resource_type like '%s%%')";
  private Jdbi jdbi;

  @Override
  public void init(MagpieConfig config) {
    final var rawPersistConfig = config.getPlugins().get(PersistPlugin.ID);
    if (rawPersistConfig == null) {
      throw new ConfigException(String.format("Config file does not contain %s configuration", PersistPlugin.ID));
    }

    try {
      final PersistConfig persistConfig = MAPPER.treeToValue(MAPPER.valueToTree(rawPersistConfig.getConfig()), PersistConfig.class);

      String url = String.format("jdbc:postgresql://%s:%s/%s", persistConfig.getHostname(), persistConfig.getPort(), persistConfig.getDatabaseName());
      jdbi = Jdbi.create(url, persistConfig.getUser(), persistConfig.getPassword())
        .installPlugin(new PostgresPlugin());
    } catch (JsonProcessingException e) {
      throw new ConfigException("Cannot instantiate PersistConfig while initializing PolicyAnalyzerService", e);
    }
  }

  @Override
  public ScanResults analyze(List<PolicyContext> policies) throws Exception {
    Map<PolicyContext, List<Violation>> violations = new HashMap<>();
    Map<PolicyContext, Map<Rule, ScanResults.IgnoredReason>> ignoredRules = new HashMap<>();
    AtomicInteger numOfViolations = new AtomicInteger();

    policies.forEach(policyContext -> {
      List<Violation> policyViolations = new ArrayList<>();
      Map<Rule, ScanResults.IgnoredReason> policyIgnoredRules = new HashMap<>();
      final var policy = policyContext.getPolicy();

      processPolicy(policy, numOfViolations, policyViolations, policyIgnoredRules);

      if (!policyViolations.isEmpty()) {
        violations.put(policyContext, policyViolations);
      }
      if (!policyIgnoredRules.isEmpty()) {
        ignoredRules.put(policyContext, policyIgnoredRules);
      }
    });
    return new ScanResults(policies, violations, ignoredRules, numOfViolations.get());
  }

  private void processPolicy(Policy policy,
                             AtomicInteger numOfViolations,
                             List<Violation> policyViolations,
                             Map<Rule, ScanResults.IgnoredReason> policyIgnoredRules) {
    if (!policy.isEnabled()) { // Not enabled
      LOGGER.info("Policy '{}' disabled", policy.getPolicyName());
      return;
    }

    if (!cloudProviderAssetsAvailable(policy)) { // No cloud related assets
      LOGGER.warn("There was no assets detected for cloudProvider: {}, policy: {}",
        policy.getCloudProvider(), policy.getPolicyName());
      return;
    }

    LOGGER.info("Analyzing policy - {}", policy.getPolicyName());
    policy.getRules().forEach(rule -> {
      executeRule(numOfViolations, policyViolations, policyIgnoredRules, policy, rule);
    });
  }

  private boolean cloudProviderAssetsAvailable(Policy policy) {
    return jdbi.withHandle(handle -> handle.createQuery(String.format(EXIST_ASSETS_PER_CLOUD, policy.getCloudProvider()))
      .mapTo(Boolean.class).findFirst().orElseThrow());
  }

  private void executeRule(AtomicInteger numOfViolations,
                           List<Violation> policyViolations,
                           Map<Rule, ScanResults.IgnoredReason> policyIgnoredRules,
                           Policy policy,
                           Rule rule) {
    if (!rule.isEnabled()) {
      policyIgnoredRules.put(rule, ScanResults.IgnoredReason.DISABLED);
      LOGGER.info("Rule '{}' disabled", rule.getRuleName());
      return;
    }

    if (rule.isManualControl()) {
      policyIgnoredRules.put(rule, ScanResults.IgnoredReason.MANUAL_CONTROL);
      LOGGER.info("Rule not analyzed (manually controlled) - {}", rule.getRuleName());
      return;
    }

    var missingAssets = checkForMissingAssets(jdbi, rule.getSql());
    if (!missingAssets.isEmpty()) { // Missing assets found
      policyIgnoredRules.put(rule, ScanResults.IgnoredReason.MISSING_ASSET);
      LOGGER.info("Missing assets for analyzing the rule, ignoring. [assets={}, rule={}]", missingAssets, rule.getRuleName());
      return;
    }

    LOGGER.info("Analyzing rule - {}", rule.getRuleName());
    LocalDateTime evaluatedAt = LocalDateTime.now();

    var results = jdbi.withHandle(handle -> handle.createQuery(rule.getSql()).mapToMap().list());

    StringWriter evalOut = new StringWriter();
    StringWriter evalErr = new StringWriter();
    if (!Optional.ofNullable(rule.getEval()).orElse("").isEmpty()) {
      try {
        evalOut.append(evaluate(rule, results));
      } catch (Exception e) {
        evalErr.append(e.getMessage());
      }
    }

    results.forEach(result -> {
      Violation violation = new Violation();
      violation.setPolicyId(policy.getId());
      violation.setRuleId(rule.getId());
      violation.setAssetId(result.get("asset_id").toString());
      // TODO : Policy description or rule description
      violation.setInfo(policy.getDescription() + (evalOut.toString().isEmpty() ? "" : "\nEvaluation output:\n" + evalOut));
      violation.setError(evalErr.toString());
      violation.setEvaluatedAt(evaluatedAt);
      policyViolations.add(violation);

      numOfViolations.getAndIncrement();
    });

  }

  private List<String> checkForMissingAssets(Jdbi jdbi, String sql) {
    String sqlNoWhitespaces = sql.replaceAll("\\s+", "");
    String resourceTypeSearch = "resource_type='";
    List<String> resourceTypes = new ArrayList<>();

    int index = 0;
    while (index != -1) {
      index = sqlNoWhitespaces.indexOf(resourceTypeSearch, index);
      if (index != -1) {
        int resourceTypeEndIndex = sqlNoWhitespaces.indexOf('\'', index + resourceTypeSearch.length());
        String resourceType = sqlNoWhitespaces.substring(index + resourceTypeSearch.length(), resourceTypeEndIndex);
        resourceTypes.add(resourceType);
        index++;
      }
    }

    List<String> missingAssets = new ArrayList<>();
    resourceTypes.forEach(resourceType -> {
      String query = String.format("SELECT COUNT(*) FROM assets WHERE resource_type ='%s';", resourceType);

      var results = jdbi.withHandle(handle -> {
        return handle.createQuery(query).mapToMap().list();
      });

      if ((long) results.get(0).get("count") == 0) {
        missingAssets.add(resourceType);
      }
    });

    return missingAssets;
  }

  @Override
  public String evaluate(Rule rule, Object resultSet) throws Exception {
    StringWriter output = new StringWriter();
    try (PythonInterpreter pyInterp = new PythonInterpreter()) {
      pyInterp.setOut(output);

      // Define evaluate method
      pyInterp.exec(rule.getEval());

      // Prepare argument and execute evaluate()
      pyInterp.set("resultset", resultSet);
      pyInterp.exec("evaluate(resultset)");
    } catch (Exception e) {
      throw new Exception(e);
    }
    return output.toString();
  }
}
