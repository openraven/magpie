package io.openraven.magpie.core.config;


import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigUtilsTest {

  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  private static class Config1 {

    public String value1 = "v1";
    public String value2 = "v2";
    public boolean isRandom = false;
  }

  private static class Config2 {
    public Map<String, String> mapping = Map.of("key1", "value1", "key2", "value2");
    public List<String> names = List.of("frick", "frack", "frock");
  }

  private static MagpieConfig defaultConfig() {
    var config = new MagpieConfig();

    var pluginConfig1 = new PluginConfig<Config1>();
    pluginConfig1.setConfig(new Config1());
    ;
    config.getPlugins().put("plugin1", pluginConfig1);


    var pluginConfig2 = new PluginConfig<Config2>();
    pluginConfig2.setConfig(new Config2());
    ;
    pluginConfig2.setEnabled(false);
    config.getPlugins().put("plugin2", pluginConfig2);

    return config;
  }

  private static Stream<Arguments> configSource() {
    return Stream.of(
      Arguments.of(defaultConfig(), new HashMap<String, String>()), // This should be a no-op
      Arguments.of(defaultConfig(), Map.of(
        "mapgie_plugins_plugin1_config_v2", "v3",
        "magpie_plugins_plugin1_config_isRandom", "true"
      )),
      Arguments.of(defaultConfig(), Map.of(
        "mapgie_plugins_plugin2_config_names", "[foo, bar]",
        "magpie_plugins_plugin1_config_v1", "v4"
      ))
    );

  }

  @ParameterizedTest
  @MethodSource("configSource")
  void testOverrides(MagpieConfig config, Map<String, String> overrides) throws IOException {
    var newConfig = ConfigUtils.merge(config, overrides);
    if (overrides.isEmpty()) {
//      assertEquals(config, newConfig);
      return;
    }

    System.out.println();
  }


  @Test
  void testEnvMapping() throws Exception {
    var env = Map.of(
      "MAGPIE_CONFIG_1", "[{'/plugins/magpie.json.output/enabled': true}, {'/plugins/magpie.aws.discovery/config/services': ['s3']}]",
      "MAGPIE_CONFIG_2", "{'/plugins/magpie.aws.discovery.2/config/services': ['ec2', 'rds']}"
    );

    var config = ConfigUtils.envOverrides(env).entrySet().stream()
      .map(e -> Map.entry(e.getKey().toString(), e.getValue()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    assertEquals(3, config.size());
    assertEquals(true, config.get("/plugins/magpie.json.output/enabled").booleanValue());
    assertEquals("s3", config.get("/plugins/magpie.aws.discovery/config/services").get(0).textValue());
    assertEquals(2, ((ArrayNode)config.get("/plugins/magpie.aws.discovery.2/config/services")).size());
  }
}
