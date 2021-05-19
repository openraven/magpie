package io.openraven.magpie.core.cspm.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.core.config.ConfigException;
import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.core.cspm.Violation;
import io.openraven.magpie.plugins.persist.PersistConfig;
import io.openraven.magpie.plugins.persist.PersistPlugin;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PolicyAnalyzerServiceImpl implements PolicyAnalyzerService {
  private static final Logger LOGGER = LoggerFactory.getLogger(PolicyAnalyzerServiceImpl.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
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
  public List<Violation> analyze(List<PolicyContext> policies) throws Exception {
    List<Violation> violations = new ArrayList<>();

    policies.forEach(policy -> {
      LOGGER.info("Analyzing policy - {}", policy.getPolicy().getName());
      policy.getPolicy().getRules().forEach(rule -> {
        LOGGER.info("Analyzing rule - {}", rule.getName());
        LocalDateTime evaluatedAt = LocalDateTime.now();

        var results = jdbi.withHandle(handle -> {
          return handle.createQuery(rule.getSql()).mapToMap().list();
        });

        results.forEach(result -> {
          Violation violation = new Violation();
          violation.setPolicyId(policy.getPolicy().getId());
          violation.setRuleId(rule.getId());
          violation.setAssetId(result.get("arn").toString());
          violation.setInfo(policy.getPolicy().getDescription());
          violation.setEvaluatedAt(evaluatedAt);
          violations.add(violation);
        });
      });
    });
    return violations;
  }
}
