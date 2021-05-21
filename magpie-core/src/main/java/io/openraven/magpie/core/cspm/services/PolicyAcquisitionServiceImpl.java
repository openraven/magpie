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

    var policyContexts = new ArrayList<PolicyContext>();

    policyConfig.getRepositories()
      .stream()
      .map(repository -> repository.replace("~", System.getProperty("user.home")))
      .forEach(repository -> {
        String repositoryPath = getTargetProjectDirectoryPath(repository).toString();
        File policiesDirectory = new File(repositoryPath + "/policies");
        File rulesDirectory = new File(repositoryPath + "/rules");

        HashMap<String, Rule> repositoryRulesMap = loadRulesFromDirectory(rulesDirectory);

        var repositoryPolicies = loadPoliciesFromDirectory(policiesDirectory, repositoryRulesMap);
        policyContexts.addAll(repositoryPolicies);
      });

    return policyContexts;
  }

  private HashMap<String, Rule>  loadRulesFromDirectory(File rulesDirectory ) {
    var rules = new HashMap<String, Rule>();

    if(rulesDirectory.exists()) {
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

  private ArrayList<PolicyContext> loadPoliciesFromDirectory(File policiesDirectory, HashMap<String, Rule> repositoryRulesMap) {
    var policiesContexts = new ArrayList<PolicyContext> ();

    if(policiesDirectory.exists()) {
      for (File policyRule : Objects.requireNonNull(policiesDirectory.listFiles())) {
        var policy = readPolicy(policyRule);
        if(policy != null) {
          for ( Rule rule : policy.getPolicy().getRules()) {
            if(repositoryRulesMap.containsKey(rule.getId())) {
              rule.set(repositoryRulesMap.get(rule.getId()));
            } else {
              LOGGER.error("Rule not found {}", rule.getId());
            }
          }
          policiesContexts.add(policy);
        }
      }
    } else {
      LOGGER.error("Policies directory {} doesn't exists!", policiesDirectory);
    }

    return policiesContexts;
  }

  private PolicyContext readPolicy(File file) {
    try {
      Policy policy = YAML_MAPPER.readValue(file, Policy.class);

      LOGGER.info("Successfully loaded policy: {}", policy.getId());

      var policyMetadata = new PolicyMetadata(file.getPath(), "");

      return new PolicyContext(policyMetadata, policy);
    } catch (IOException yamlIOException) {
      LOGGER.error(yamlIOException.getMessage());

      return null;
    }
  }

  private void getGitRepository(String repository) {
    String targetPath = getTargetProjectDirectoryPath(repository).toString();

    if (Files.exists(Path.of(targetPath))) {
      executeShellCommand(Arrays.asList("git", "pull"), targetPath);
    } else {
      executeShellCommand(Arrays.asList("git", "clone", repository, targetPath), null);
    }
  }

  private void copyLocalRepository(String repository) {
    Path scrPath = Path.of(repository);
    Path targetProjectDirectoryPath = getTargetProjectDirectoryPath(repository);

    try {
      File sourceDirectory = scrPath.toFile();
      File destinationDirectory = targetProjectDirectoryPath.toFile();

      FileUtils.deleteDirectory(destinationDirectory);
      FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

      LOGGER.info("Successfully copied {} to {}", scrPath, targetProjectDirectoryPath);
    } catch (IOException e) {
      LOGGER.error(e.getMessage());
    }
  }

  private void executeShellCommand(List<String> command, String directory) {
    try {
      ProcessBuilder processBuilder = new ProcessBuilder();
      processBuilder.command(command);
      if (directory != null) {
        processBuilder.directory(new File(directory));
      }

      Process process = processBuilder.start();

      String stdout = IOUtils.toString(process.getErrorStream(), Charset.defaultCharset());
      String stderr = IOUtils.toString(process.getInputStream(), Charset.defaultCharset());

      LOGGER.info(stdout);
      LOGGER.info(stderr);
    } catch (IOException e) {
      LOGGER.error(e.getMessage());
    }
  }

  private String getProjectNameFromRepository(String repository) {
    return repository
      .replaceAll(".*/", "")
      .replace(".git", "");
  }

  private Path getTargetProjectDirectoryPath(String repository) {
    return Path.of(policyConfig.getRoot().replace("~", System.getProperty("user.home"))
      + "/"
      + getProjectNameFromRepository(repository));
  }
}
