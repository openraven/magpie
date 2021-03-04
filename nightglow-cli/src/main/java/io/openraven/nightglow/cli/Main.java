package io.openraven.nightglow.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.openraven.nightglow.api.Session;
import io.openraven.nightglow.core.Orchestrator;
import io.openraven.nightglow.core.config.NightglowConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

public class Main {

  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  public static String humanReadableFormat(Duration duration) {
    return duration.toString()
      .substring(2)
      .replaceAll("(\\d[HMS])(?!$)", "$1 ")
      .toLowerCase();
  }

  public static void main(String[] args) throws IOException {
    final var start = Instant.now();
    try(var is = new FileInputStream((new File("config.yaml")))) {
      final var config = MAPPER.readValue(is, NightglowConfig.class);
      LOGGER.info("OSS Discovery. Classpath={}", System.getProperties().get("java.class.path"));
      new Orchestrator(config, new Session()).scan();
    }
    LOGGER.info("Discovery completed in {}", humanReadableFormat(Duration.between(start, Instant.now())));
  }
}
