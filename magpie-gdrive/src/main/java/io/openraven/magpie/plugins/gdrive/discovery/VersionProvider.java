package io.openraven.magpie.plugins.gdrive.discovery;

import io.openraven.magpie.plugins.gdrive.discovery.exception.GDriveDiscoveryException;

import java.io.IOException;
import java.util.Properties;

public class VersionProvider {
  private static final String RESOURCE = "version.properties";
  private final String projectVersion;

  public VersionProvider() {

    Properties properties = new Properties();
    try {
      properties.load(getClass().getClassLoader().getResourceAsStream(RESOURCE));
    } catch (IOException ex) {
      throw new GDriveDiscoveryException(String.format("Unable to load: %s from resources", RESOURCE), ex);
    }

    projectVersion = properties.getProperty("project.version");
  }

  public String getProjectVersion() {
    return projectVersion;
  }
}
