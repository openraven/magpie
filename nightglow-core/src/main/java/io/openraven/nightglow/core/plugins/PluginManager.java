package io.openraven.nightglow.core.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.nightglow.api.EnumerationPlugin;
import io.openraven.nightglow.api.IntermediatePlugin;
import io.openraven.nightglow.api.NightglowPlugin;
import io.openraven.nightglow.api.TerminalPlugin;
import io.openraven.nightglow.core.config.NightglowConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public class PluginManager {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final Logger LOGGER = LoggerFactory.getLogger(PluginManager.class);
  private static final List<Class<? extends NightglowPlugin>> PLUGIN_CLASSES = List.of(EnumerationPlugin.class, IntermediatePlugin.class, TerminalPlugin.class);

  private final NightglowConfig config;

  private final Map<Class<? extends NightglowPlugin>, List<NightglowPlugin<?>>> plugins = new HashMap<>();

  public PluginManager(NightglowConfig config) {
    this.config = config;
    loadPlugins();
  }

  private void loadPlugins() {
    PLUGIN_CLASSES.forEach(c -> {
      final var loader = ServiceLoader.load(c);
      loader.stream().forEach(svc -> {
        final var plugin = svc.get();
        try {
          final var configType = plugin.configType();
          final var pluginConfig = buildPluginConfig(configType, plugin.configType());
          plugin.init(pluginConfig, LoggerFactory.getLogger(plugin.configType()));
          var pluginList = plugins.getOrDefault(c, List.of());
          pluginList.add(plugin);
          plugins.put(c, pluginList);
          LOGGER.debug("Loaded {}", plugin.id());
        } catch (Exception ex) {
          LOGGER.warn("Failed to load {}", plugin.id(), ex);
        }
      });
    });
  }

  private Object buildPluginConfig(Class<?> configType, Object config) throws JsonProcessingException {
    return MAPPER.treeToValue(MAPPER.valueToTree(config), configType);
  }

  public List<NightglowPlugin<?>> ofType(Class<? extends NightglowPlugin> clazz) {
    final var list = plugins.get(clazz);
    return list == null ? Collections.emptyList() :  Collections.unmodifiableList(list);
  }
}
