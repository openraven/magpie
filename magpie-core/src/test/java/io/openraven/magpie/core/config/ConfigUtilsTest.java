package io.openraven.magpie.core.config;


import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigUtilsTest {

  @Test
  void testEnvMapping() {
    var env = Map.of(
      "MAGPIE_CONFIG_1", "[{'/plugins/magpie.json.output/enabled': true}, {'/plugins/magpie.aws.discovery/config/services': ['s3']}]",
      "MAGPIE_CONFIG_2", "{'/plugins/magpie.aws.discovery.2/config/services': ['ec2', 'rds']}"
    );

    var config = ConfigUtils.envOverrides(env).entrySet().stream()
      .map(e -> Map.entry(e.getKey().toString(), e.getValue()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    assertEquals(3, config.size());
    assertTrue(config.get("/plugins/magpie.json.output/enabled").booleanValue());
    assertEquals("s3", config.get("/plugins/magpie.aws.discovery/config/services").get(0).textValue());
    assertEquals(2, config.get("/plugins/magpie.aws.discovery.2/config/services").size());
  }
}
