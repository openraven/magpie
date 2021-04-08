package io.openraven.magpie.plugins.aws.discovery;

import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class VersioningEmitterWrapper {
  private Emitter emitter;
  private String awsSdkVersion;
  private String projectVersion;

  private VersioningEmitterWrapper() {
  }

  public VersioningEmitterWrapper(Emitter emitter) {
    this.emitter = emitter;

    Properties properties = new Properties();
    try {
      properties.load(getClass().getClassLoader().getResourceAsStream("properties-from-pom.properties"));
    } catch (IOException e) {
      e.printStackTrace();
    }

    awsSdkVersion = properties.getProperty("aws.sdk.version");
    projectVersion = properties.getProperty("project.version");
  }

  public void emit(MagpieEnvelope envelope) {
    Map<String, String> metadata = envelope.getMetadata();
    metadata.put("magpie.aws.version", projectVersion);
    metadata.put("aws.sdk.version", awsSdkVersion);
    envelope.setMetadata(metadata);

    emitter.emit(envelope);
  }
}
