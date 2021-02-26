package io.openraven.nightglow.core.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.openraven.nightglow.api.EnumerationPlugin;
import io.openraven.nightglow.core.config.NightglowConfig;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluginManagerTest {

  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  @Test
  public void testSimpleConfig() throws Exception {
    try(InputStream is = PluginManagerTest.class.getResourceAsStream("/pluginconfig/simple.yaml")) {
      final var config = MAPPER.readValue(is, NightglowConfig.class);

      assertEquals(2, config.getPlugins().size());
      final var pluginManager = new PluginManager(config);
      assertEquals(2, pluginManager.byType(EnumerationPlugin.class).size());

      var plugin = (NightglowTestPlugin)pluginManager.byType(EnumerationPlugin.class).get(0);
      assertEquals("deadbeef", plugin.config.getApiKey());
      assertEquals("cafebabe", plugin.config.getApiSecret());
    }
  }
}
