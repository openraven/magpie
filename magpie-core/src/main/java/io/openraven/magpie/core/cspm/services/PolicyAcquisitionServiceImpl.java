package io.openraven.magpie.core.cspm.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.core.config.PolicyConfig;
import io.openraven.magpie.core.cspm.Policy;
import io.openraven.magpie.core.cspm.Rule;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class PolicyAcquisitionServiceImpl implements PolicyAcquisitionService {
  private static final Logger LOGGER = LoggerFactory.getLogger(PolicyAcquisitionServiceImpl.class);
  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
  private PolicyConfig policyConfig;

  @Override
  public void init(MagpieConfig config) {
    policyConfig = config.getPolicies();

    policyConfig.getRepositories()
      .forEach(repository -> {
        if (repository.startsWith("github") || repository.startsWith("http") || repository.startsWith("git")) {
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

    List<PolicyContext> policyContexts = List.of();

    policyConfig.getRepositories()
      .stream()
      .map(repository -> repository.replace("~", System.getProperty("user.home")))
      .forEach(repository -> {
      String repositoryPath = policyConfig.getRoot().replace("~", System.getProperty("user.home")) + "/" + getProjectNameFromRepository(repository);

      File directory = new File(repositoryPath + "/Policies");

      for (File file : Objects.requireNonNull(directory.listFiles())) {
        try {
          Policy policy = YAML_MAPPER.readValue(file, Policy.class);

          LOGGER.info("Successfully loaded policy: {}", policy.getId());
          for (Rule rule : policy.getRules()) {
            File ruleFile = new File(repositoryPath + "/Rules/" + rule.getId() + ".yaml");

            try {
              Rule yamlRule = YAML_MAPPER.readValue(ruleFile, Rule.class);
              rule.set(yamlRule);
              LOGGER.info("Successfully loaded rule {} for policy: {}", rule.getId(), policy.getName());
            } catch (IOException yamlIOException) {
              LOGGER.error(yamlIOException.getMessage());
            }
          }
        } catch (IOException yamlIOException) {
          LOGGER.error(yamlIOException.getMessage());
        }
      }
    });

    return policyContexts;
  }

  private void getGitRepository(String repository) {
    Path path = Paths.get(policyConfig.getRoot().replace("~", System.getProperty("user.home")) + "/" + getProjectNameFromRepository(repository));

    if (Files.exists(path)) {
      executeShellCommand(String.format("git clone %s %s", repository, path));
    } else {
      executeShellCommand(String.format("git clone %s %s", repository, path));
    }
  }

  private void copyLocalRepository(String repository) {
    Path scrPath = Path.of(repository);
    Path targetProjectDirectoryPath = Path.of(policyConfig.getRoot().replace("~", System.getProperty("user.home")) + "/" + getProjectNameFromRepository(repository));

    try {
      File sourceDirectory = scrPath.toFile();
      File destinationDirectory = targetProjectDirectoryPath.toFile();
      FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

      LOGGER.info("Successfully copied {} to {}", scrPath, targetProjectDirectoryPath);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private String getProjectNameFromRepository(String repository) {
    return repository
      .replaceAll(".*/", "")
      .replace(".git", "");
  }

  private void executeShellCommand(String command) {
    LOGGER.info(command);

    try {
      Process process = Runtime.getRuntime().exec(command);

      BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));

      String line;
      while ((line = input.readLine()) != null) {
        LOGGER.info("Line: " + line);
      }
    } catch (IOException e) {
      LOGGER.error(e.getMessage());
    }
  }
}
