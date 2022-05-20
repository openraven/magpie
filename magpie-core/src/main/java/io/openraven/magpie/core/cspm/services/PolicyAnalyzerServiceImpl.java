package io.openraven.magpie.core.cspm.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.core.config.ConfigException;
import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.core.cspm.analysis.IgnoredRule;
import io.openraven.magpie.core.cspm.analysis.ScanResults;
import io.openraven.magpie.core.cspm.analysis.Violation;
import io.openraven.magpie.core.cspm.model.Policy;
import io.openraven.magpie.core.cspm.model.PolicyContext;
import io.openraven.magpie.core.cspm.model.Rule;
import io.openraven.magpie.plugins.persist.AssetsRepo;
import io.openraven.magpie.plugins.persist.PersistConfig;
import io.openraven.magpie.plugins.persist.PersistPlugin;
import io.openraven.magpie.plugins.persist.impl.HibernateAssetsRepoImpl;
import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.google.common.base.Strings;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;

import static io.openraven.magpie.core.cspm.analysis.IgnoredRule.IgnoredReason.*;

public class PolicyAnalyzerServiceImpl implements PolicyAnalyzerService {
  private static final Logger LOGGER = LoggerFactory.getLogger(PolicyAnalyzerServiceImpl.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private AssetsRepo assetsRepo;

  @Override
  public void init(MagpieConfig config) {
    final var rawPersistConfig = config.getPlugins().get(PersistPlugin.ID);
    if (rawPersistConfig == null) {
      throw new ConfigException(String.format("Config file does not contain %s configuration", PersistPlugin.ID));
    }

    try {
      final PersistConfig persistConfig = MAPPER.treeToValue(MAPPER.valueToTree(rawPersistConfig.getConfig()), PersistConfig.class);
      assetsRepo = new HibernateAssetsRepoImpl(persistConfig);
    } catch (JsonProcessingException e) {
      throw new ConfigException("Cannot instantiate PersistConfig while initializing PolicyAnalyzerService", e);
    }
  }

  @Override
  public ScanResults analyze(List<PolicyContext> policyContexts) {
    List<Violation> violations = new ArrayList<>();
    List<IgnoredRule> ignoredRules = new ArrayList<>();
    List<Policy> policies = new ArrayList<>();

    policyContexts.forEach(policyContext -> {
      final var policy = policyContext.getPolicy();
      processPolicy(policy, violations, ignoredRules);
      policies.add(policy);
    });
    return new ScanResults(policies, violations, ignoredRules);
  }

  private void processPolicy(Policy policy,
                             List<Violation> policyViolations,
                             List<IgnoredRule> policyIgnoredRules) {
    if (!policy.isEnabled()) { // Not enabled
      LOGGER.info("Policy '{}' disabled", policy.getName());
      return;
    }

    if (!cloudProviderAssetsAvailable(policy)) { // No cloud related assets
      LOGGER.warn("No assets found for cloudProvider: {}, policy: {}", policy.getCloudProvider(), policy.getName());
      return;
    }

    LOGGER.info("Analyzing policy - {}", policy.getName());
    policy.getRules().forEach(rule -> executeRule(policyViolations, policyIgnoredRules, policy, rule));
  }

  private boolean cloudProviderAssetsAvailable(Policy policy) {
    var provider = Strings.isNullOrEmpty(policy.getCloudProvider()) ? "" : policy.getCloudProvider().toLowerCase(Locale.ROOT);
    List<Map<String, Object>> data = assetsRepo.queryNative("select count(*) from magpie.%provider%".replace("%provider%", provider));
    return BigInteger.ZERO.compareTo((BigInteger)data.get(0).get("count")) < 0;
  }

  protected void executeRule(List<Violation> policyViolations,
                           List<IgnoredRule> policyIgnoredRules,
                           Policy policy,
                           Rule rule) {
    if (!rule.isEnabled()) {
      policyIgnoredRules.add(new IgnoredRule(policy, rule, DISABLED));
      LOGGER.info("Rule '{}' disabled", rule.getName());
      return;
    }

    if (rule.isManualControl()) {
      policyIgnoredRules.add(new IgnoredRule(policy, rule, MANUAL_CONTROL));
      LOGGER.info("Rule not analyzed (manually controlled) - {}", rule.getName());
      return;
    }

    var missingAssets = checkForMissingAssets(rule.getSql());
    if (!missingAssets.isEmpty()) { // Missing assets found
      policyIgnoredRules.add(new IgnoredRule(policy, rule, MISSING_ASSET));
      LOGGER.info("Missing assets for analyzing the rule, ignoring. [assets={}, rule={}]", missingAssets, rule.getName());
      return;
    }

    LOGGER.info("Analyzing rule - {}", rule.getName());
    LocalDateTime evaluatedAt = LocalDateTime.now();

    List<Map<String, Object>> results = assetsRepo.queryNative(rule.getSql());

    StringWriter evalErr = new StringWriter();
    if (!Optional.ofNullable(rule.getEval()).orElse("").isEmpty()) {
      try {
        results = evaluate(rule, results);
      } catch (Exception e) {
        LOGGER.warn("Couldn't run eval code", e);
        evalErr.append(e.getMessage());
      }
    }

    results.forEach(result -> {
      Violation violation = new Violation();
      violation.setPolicy(policy);
      violation.setRule(rule);
      violation.setAssetId(result.get("assetid").toString()); // Assume Rules should always return this type of alias
      violation.setInfo(rule.getDescription());
      violation.setError(evalErr.toString());
      violation.setEvaluatedAt(evaluatedAt);
      policyViolations.add(violation);
    });
  }

  private List<String> checkForMissingAssets(String sql) {
    String sqlNoWhitespaces = sql.replaceAll("\\s+", "");
    String resourceTypeSearch = "resourcetype='";
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
      if (assetsRepo.getAssetCount(resourceType) == 0) {
        missingAssets.add(resourceType);
      }
    });

    return missingAssets;
  }

  @Override
  public List<Map<String, Object>> evaluate(Rule rule, Object resultSet) throws Exception {

    final var results = new ArrayList<Map<String, Object>>();

    StringWriter output = new StringWriter();
    try (PythonInterpreter pyInterp = new PythonInterpreter()) {
      pyInterp.setOut(output);

      // Define evaluate method
      pyInterp.exec(rule.getEval());
      // Prepare argument and execute evaluate()
      pyInterp.set("resultset", resultSet);
      var v = pyInterp.eval("evaluate(resultset)");
      if (v instanceof PyList) {
        ((PyList)v).stream().forEach(item -> {
          if (item instanceof PyDictionary) {
            final var dict = (PyDictionary)item;
            final var map = new HashMap<String, Object>();
            dict.keySet().forEach(k -> map.put(k.toString(), dict.get(k)));
            results.add(map);
          } else {
            LOGGER.warn("{} returned an invalid value, found {} but expected a dictionary", rule.getName(), item.getClass().getName());
          }
        });
      } else {
        throw new RuntimeException("Eval block returned an illegal value. Expected a Python list but found " + v.getClass().getName());
      }
    } catch (Exception e) {
      throw new Exception(e);
    }
    return results;
  }
}
