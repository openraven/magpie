package io.openraven.magpie.plugins.aws.discovery;

import java.io.IOException;
import java.util.Properties;

public class VersionProvider {
  private final String awsSdkVersion;
  private final String projectVersion;
  
  public VersionProvider() {

    Properties properties = new Properties();
    try {
      properties.load(getClass().getClassLoader().getResourceAsStream("properties-from-pom.properties"));
    } catch (IOException e) {
      e.printStackTrace();
    }

    awsSdkVersion = properties.getProperty("aws.sdk.version");
    projectVersion = properties.getProperty("project.version");
  }

  public String getAwsSdkVersion() {
    return awsSdkVersion;
  }

  public String getProjectVersion() {
    return projectVersion;
  }
}
