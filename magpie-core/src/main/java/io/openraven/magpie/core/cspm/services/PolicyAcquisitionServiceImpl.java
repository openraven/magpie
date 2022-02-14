package io.openraven.magpie.core.cspm.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.core.config.PolicyConfig;
import io.openraven.magpie.core.cspm.model.PolicyContext;
import io.openraven.magpie.core.cspm.model.PolicyMetadata;
import io.openraven.magpie.core.cspm.model.Rule;
import io.openraven.magpie.core.cspm.model.Policy;
import io.openraven.magpie.plugins.persist.PersistConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class PolicyAcquisitionServiceImpl implements PolicyAcquisitionService {

  private static final String SQL_SCHEMA_TOKEN = "${magpie_schema}";

  private static final Logger LOGGER = LoggerFactory.getLogger(PolicyAcquisitionServiceImpl.class);
  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
  private PolicyConfig policyConfig;
  private MagpieConfig config;

  @Override
  public void init(MagpieConfig config) {
    policyConfig = config.getPolicies();
    this.config = config;

    policyConfig.getRepositories()
      .stream()
      .map(repository -> repository.replace("~", System.getProperty("user.home")))
      .forEach(repository -> {
        if (isGitRepository(repository)) {
          getGitRepository(repository);
        } else {
          copyLocalRepository(repository);
        }
      });
  }

  @Override
  public List<PolicyContext> loadPolicies() {
    if (policyConfig == null) {
      LOGGER.error("Class have to be initialized first!");
      return List.of();
    }

    var policyContexts = new ArrayList<PolicyContext>();

    policyConfig.getRepositories()
      .stream()
      .map(repository -> repository.replace("~", System.getProperty("user.home")))
      .forEach(repository -> {
        String repositoryPath = getTargetProjectDirectoryPath(repository).toString();

        var repositoryPolicies = loadPoliciesFromRepository(repositoryPath);
        policyContexts.addAll(repositoryPolicies);
      });

    return policyContexts;
  }

  private List<Rule> loadRules(String rulesDirectory, List<String> ruleFileNames) {
    List<Rule> rules = new ArrayList<>();
    final var persistConfig = config.getPlugins().get("magpie.persist");
    final var schema = ((PersistConfig)persistConfig.getConfig()).getSchema();

    for (String ruleFileName : ruleFileNames) {
      try {
        File ruleFile = new File(rulesDirectory + "/" + ruleFileName);
        Rule yamlRule = YAML_MAPPER.readValue(ruleFile, Rule.class);

        // Loading post-processing.  Update the file name and modify the sql to replace the SQL schema placeholder
        yamlRule.setFileName(ruleFileName);
        yamlRule.setSql(yamlRule.getSql().replaceAll(SQL_SCHEMA_TOKEN, schema));

        rules.add(yamlRule);
        LOGGER.info("Successfully loaded rule {}", yamlRule.getRuleId());
      } catch (IOException yamlIOException) {
        LOGGER.error(yamlIOException.getMessage());
      }
    }

    return rules;
  }

  private ArrayList<PolicyContext> loadPoliciesFromRepository(String repositoryPath) {
    File policiesDirectory = new File(repositoryPath + "/policies");
    File rulesDirectory = new File(repositoryPath + "/rules");

    var policiesContexts = new ArrayList<PolicyContext>();

    if (policiesDirectory.exists() && rulesDirectory.exists()) {
      for (File policyFile : Objects.requireNonNull(policiesDirectory.listFiles())) {
        var policy = loadPolicy(policyFile);

        if (policy != null) {
          var policyRulesFiles = policy.getRules()
            .stream()
            .map(Rule::getId)
            .collect(Collectors.toList());

          policy.setRules(loadRules(rulesDirectory.toString(), policyRulesFiles));

          var policyMetadata = new PolicyMetadata(policyFile.getPath(), getRepoHashOrLocalRepositoryString(repositoryPath));
          policiesContexts.add(new PolicyContext(policyMetadata, policy));
        }
      }
    } else {
      if (!policiesDirectory.exists()) {
        LOGGER.error("Policies directory {} doesn't exists!", policiesDirectory);
      }
      if (!rulesDirectory.exists()) {
        LOGGER.error("Rules directory {} doesn't exists!", rulesDirectory);
      }
    }

    return policiesContexts;
  }

  private ArrayList<Rule> getRulesFromRulesMap(Map<String, Rule> repositoryRulesMap, List<String> rulesIds) {
    var rules = new ArrayList<Rule>();

    rulesIds.forEach(rule -> {
      if (repositoryRulesMap.containsKey(rule)) {
        rules.add(repositoryRulesMap.get(rule));
      } else {
        LOGGER.error("Rule not found {}", rule);
      }
    });

    return rules;
  }

  private Policy loadPolicy(File file) {
    try {
      Policy policy = YAML_MAPPER.readValue(file, Policy.class);

      LOGGER.info("Successfully loaded policy: {}", policy.getPolicyId());

      return policy;
    } catch (IOException yamlIOException) {
      LOGGER.error(yamlIOException.getMessage());
      return null;
    }
  }

  private String getRepoHashOrLocalRepositoryString(String repository) {
    String repoHash;
    if (new File(repository + "/.git").exists()) {
      repoHash = getRepoHash(new File(repository));
    } else {
      repoHash = "Local repository";
    }
    return repoHash;
  }

  private String getRepoHash(File directory) {
    return executeShellCommand(Arrays.asList("git", "rev-parse", "HEAD"), directory.toString())
      .replace(System.lineSeparator(), "");
  }

  private void getGitRepository(String repository) {
    Path targetPath = getTargetProjectDirectoryPath(repository);

    if (Files.exists(targetPath)) {
      executeShellCommand(Arrays.asList("git", "pull"), targetPath.toString());
    } else {
      executeShellCommand(Arrays.asList("git", "clone", repository, targetPath.toString()), null);
    }
  }

  private void copyLocalRepository(String repository) {
    try {
      File sourceDirectory = new File(repository);
      File destinationDirectory = getTargetProjectDirectoryPath(repository).toFile();

      FileUtils.deleteDirectory(destinationDirectory);
      FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

      LOGGER.info("Successfully copied {} to {}", sourceDirectory, destinationDirectory);
    } catch (IOException e) {
      LOGGER.error(e.getMessage());
    }
  }

  private String executeShellCommand(List<String> command, String directory) {
    LOGGER.info("Running {}", String.join(" ", command));
    try {
      ProcessBuilder processBuilder = new ProcessBuilder();
      processBuilder.command(command);
      if (directory != null) {
        processBuilder.directory(new File(directory));
      }

      Process process = processBuilder.start();

      String stdout = IOUtils.toString(process.getInputStream(), Charset.defaultCharset());
      String stderr = IOUtils.toString(process.getErrorStream(), Charset.defaultCharset());

      LOGGER.info("Standard output: {}", stdout);
      if (!stderr.isEmpty()) {
        LOGGER.info("Error output: {}", stderr);
      }

      return stdout;
    } catch (IOException e) {
      LOGGER.error(e.getMessage());
    }

    return "";
  }

  private String getProjectNameFromRepository(String repository) {
    return repository
      .replaceAll(".*/", "")
      .replace(".git", "");
  }

  protected Path getTargetProjectDirectoryPath(String repository) {
    if (isGitRepository(repository)) {
      String[] tokens = repository
        .replace("git@", "")
        .replace("https://", "")
        .replace(":", "/")
        .split("/");
      return
        Path.of(String.format(
          "%s/%s/%s/%s",
          policyConfig.getRoot().replace("~", System.getProperty("user.home")),
          tokens[0],
          tokens[1],
          getProjectNameFromRepository(repository)));
    } else {
      return Path.of(String.format(
        "%s/%s",
        policyConfig.getRoot().replace("~", System.getProperty("user.home")),
        getProjectNameFromRepository(repository)));
    }
  }

  private boolean isGitRepository(String repository) {
    return repository.startsWith("git@") || repository.startsWith("https://");
  }
}
