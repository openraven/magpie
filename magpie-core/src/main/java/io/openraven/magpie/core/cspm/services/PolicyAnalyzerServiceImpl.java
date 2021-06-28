package io.openraven.magpie.core.cspm.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.core.config.ConfigException;
import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.core.cspm.Rule;
import io.openraven.magpie.core.cspm.ScanResults;
import io.openraven.magpie.core.cspm.Violation;
import io.openraven.magpie.plugins.persist.PersistConfig;
import io.openraven.magpie.plugins.persist.PersistPlugin;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PolicyAnalyzerServiceImpl implements PolicyAnalyzerService {
  private static final Logger LOGGER = LoggerFactory.getLogger(PolicyAnalyzerServiceImpl.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Pattern MISSING_TABLE_PATTERN = Pattern.compile("\"(\\S+?)\" does not exist");
  private Jdbi jdbi;

  @Override
  public void init(MagpieConfig config) {
    // TODO(stefan): Not nice - public ID. Getter not static. Fix this somehow
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

    policies.forEach(policy -> {
        List<Violation> policyViolations = new ArrayList<>();
        Map<Rule, ScanResults.IgnoredReason> policyIgnoredRules = new HashMap<>();
        final var p = policy.getPolicy();
        if (p.isEnabled()) {
          LOGGER.info("Analyzing policy - {}", p.getPolicyName());
          p.getRules().forEach(rule -> {
            if (rule.isEnabled()) {
              if (rule.isManualControl()) {
                policyIgnoredRules.put(rule, ScanResults.IgnoredReason.MANUAL_CONTROL);
                LOGGER.info("Rule not analyzed (manually controlled) - {}", rule.getRuleName());
              } else {
                LOGGER.info("Analyzing rule - {}", rule.getRuleName());
                LocalDateTime evaluatedAt = LocalDateTime.now();

                var missingAssets = checkForMissingAssets(jdbi, rule.getSql());
                if (missingAssets.isEmpty()) {

                  var results = jdbi.withHandle(handle -> {
                    return handle.createQuery(rule.getSql()).mapToMap().list();
                  });

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
                    violation.setPolicyId(p.getId());
                    violation.setRuleId(rule.getId());
                    violation.setAssetId(result.get("asset_id").toString());
                    violation.setInfo(p.getDescription() + (evalOut.toString().isEmpty() ? "" : "\nEvaluation output:\n" + evalOut));
                    violation.setError(evalErr.toString());
                    violation.setEvaluatedAt(evaluatedAt);
                    policyViolations.add(violation);

                    numOfViolations.getAndIncrement();
                  });
                } else {
                  policyIgnoredRules.put(rule, ScanResults.IgnoredReason.MISSING_ASSET);
                  LOGGER.info("Missing assets for analyzing the rule, ignoring. [assets={}, rule={}]", missingAssets, rule.getRuleName());
                }
              }
            } else {
              policyIgnoredRules.put(rule, ScanResults.IgnoredReason.DISABLED);
              LOGGER.info("Rule '{}' disabled", rule.getRuleName());
            }
          });
        } else {
          LOGGER.info("Policy '{}' disabled", p.getPolicyName());
        }
        if (!policyViolations.isEmpty()) {
          violations.put(policy, policyViolations);
        }
        if (!policyIgnoredRules.isEmpty()) {
          ignoredRules.put(policy, policyIgnoredRules);
        }
      }
    );
    return new ScanResults(policies, violations, ignoredRules, numOfViolations.get());
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
