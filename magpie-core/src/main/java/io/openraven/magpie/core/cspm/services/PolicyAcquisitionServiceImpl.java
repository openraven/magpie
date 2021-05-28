package io.openraven.magpie.core.cspm.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.core.config.PolicyConfig;
import io.openraven.magpie.core.cspm.Policy;
import io.openraven.magpie.core.cspm.Rule;
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
  private static final Logger LOGGER = LoggerFactory.getLogger(PolicyAcquisitionServiceImpl.class);
  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
  private PolicyConfig policyConfig;

  @Override
  public void init(MagpieConfig config) {
    policyConfig = config.getPolicies();

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

        var repositoryRulesMap = loadRulesFromRepository(repositoryPath);

        var repositoryPolicies = loadPoliciesFromRepository(repositoryPath, repositoryRulesMap);
        policyContexts.addAll(repositoryPolicies);
      });

    return policyContexts;
  }

  private Map<String, Rule> loadRulesFromRepository(String repositoryPath) {
    File rulesDirectory = new File(repositoryPath + "/rules");
    var rules = new HashMap<String, Rule>();

    if (rulesDirectory.exists()) {
      for (File ruleFile : Objects.requireNonNull(rulesDirectory.listFiles())) {
        try {
          Rule yamlRule = YAML_MAPPER.readValue(ruleFile, Rule.class);
          rules.put(yamlRule.getId(), yamlRule);
          LOGGER.info("Successfully loaded rule {}", yamlRule.getId());
        } catch (IOException yamlIOException) {
          LOGGER.error(yamlIOException.getMessage());
        }
      }
    } else {
      LOGGER.error("Rules directory {} doesn't exists!", rulesDirectory);
    }

    return rules;
  }

  private ArrayList<PolicyContext> loadPoliciesFromRepository(String repositoryPath, Map<String, Rule> repositoryRulesMap) {
    File policiesDirectory = new File(repositoryPath + "/policies");

    var policiesContexts = new ArrayList<PolicyContext>();

    if (policiesDirectory.exists()) {
      for (File policyFile : Objects.requireNonNull(policiesDirectory.listFiles())) {
        var policy = loadPolicy(policyFile);

        if (policy != null) {
          var policyRulesIds = policy.getRules()
            .stream()
            .map(Rule::getId)
            .collect(Collectors.toList());
          policy.setRules(getRulesFromRulesMap(repositoryRulesMap, policyRulesIds));

          var policyMetadata = new PolicyMetadata(policyFile.getPath(), getRepoHashOrLocalRepositoryString(repositoryPath));
          policiesContexts.add(new PolicyContext(policyMetadata, policy));
        }
      }
    } else {
      LOGGER.error("Policies directory {} doesn't exists!", policiesDirectory);
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

      LOGGER.info("Successfully loaded policy: {}", policy.getId());

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
      LOGGER.info("Error output: {}", stderr);

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

  private Path getTargetProjectDirectoryPath(String repository) {
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
