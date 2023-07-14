package io.openraven.magpie.core.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.openraven.magpie.api.IntermediatePlugin;
import io.openraven.magpie.api.MagpiePlugin;
import io.openraven.magpie.api.OriginPlugin;
import io.openraven.magpie.api.TerminalPlugin;
import io.openraven.magpie.core.config.MagpieConfig;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluginManagerTest {

  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  private static final List<Class<? extends MagpiePlugin>> DISCOVERY_PLUGIN_CLASSES =
    List.of(OriginPlugin.class, IntermediatePlugin.class, TerminalPlugin.class);

//  Disabling this test until a solution exists for proper GCPDiscovery config deserialization.
//  @Test
  public void testSimpleConfig() throws Exception {
    try(InputStream is = PluginManagerTest.class.getResourceAsStream("/pluginconfig/simple.yaml")) {
      final var config = MAPPER.readValue(is, MagpieConfig.class);

      assertEquals(2, config.getPlugins().size());
      final var pluginManager = new PluginManager(config);
      pluginManager.loadPlugins(DISCOVERY_PLUGIN_CLASSES);
      assertEquals(2, pluginManager.byType(OriginPlugin.class).size());

      var plugin = (MagpieTestPlugin)pluginManager.byType(OriginPlugin.class).get(0);
      assertEquals("deadbeef", plugin.config.getApiKey());
      assertEquals("cafebabe", plugin.config.getApiSecret());
    }
  }
}
