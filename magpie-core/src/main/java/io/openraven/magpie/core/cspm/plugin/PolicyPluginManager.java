package io.openraven.magpie.core.cspm.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.*;
import io.openraven.magpie.core.config.ConfigException;
import io.openraven.magpie.core.config.MagpieConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class PolicyPluginManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(PolicyPluginManager.class);

  private static final List<Class<? extends MagpiePlugin>> PLUGIN_CLASSES = List.of(PolicyOutputPlugin.class);

  private static final ObjectMapper MAPPER = new ObjectMapper();


  private final MagpieConfig config;

  private final Map<Class<? extends MagpiePlugin>, List<MagpiePlugin<?>>> plugins = new HashMap<>();

  public PolicyPluginManager(MagpieConfig config) {
    this.config = config;
    loadPlugins();
  }

  private void loadPlugins() {
    final var pluginsFound = new AtomicLong();
    PLUGIN_CLASSES.forEach(c -> {
      final var loader = ServiceLoader.load(c);
      var count = loader.stream().count();

      LOGGER.info("Found {} {} plugin(s) via the classpath", c.getSimpleName(), count);
      pluginsFound.addAndGet(count);
      loader.stream().forEach(svc -> {
        final var plugin = svc.get();
        try {
          final var configType = plugin.configType();
          final var pluginConfigParent = config.getPlugins().get(plugin.id());
          if (pluginConfigParent == null) {
            LOGGER.info("No configuration found for {}, ignoring.", plugin.id());

          } else if (!pluginConfigParent.isEnabled()) {
            LOGGER.info("{} found but is disabled via config. Ignoring}", plugin.id());

          } else {
            final var pluginConfig = buildPluginConfig(plugin.id(), configType, pluginConfigParent.getConfig());

            plugin.init(pluginConfig, LoggerFactory.getLogger(plugin.getClass()));
            var pluginList = plugins.getOrDefault(c, new ArrayList<>());
            pluginList.add(plugin);
            plugins.put(c, pluginList);
            LOGGER.info("Loaded {}", plugin.id());
          }
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      });
    });

    if (pluginsFound.get() == 0) {
      throw new ConfigException("No plugins found");
    }
  }

  private Object buildPluginConfig(String pluginId, Class<?> configType, Object config) throws JsonProcessingException {

    if (configType == null || "Void".equals(configType.getSimpleName())) {
      return null;
    }

    if (config == null) {
      try {
        LOGGER.debug("No config section found for {}:{}, attempting to instantiate a default", pluginId, configType.getName());
        // The plugin configuration had no defined constructor, attempt to instantiate a no-args one.
        var constructor = Arrays.stream(configType.getDeclaredConstructors()).filter(c -> c.getParameterCount() == 0).findFirst();
        if (constructor.isEmpty()) {
          LOGGER.warn("No plugin configuration found for {} and no suitable constructor found to create a default.", pluginId);
          return null;
        }
        return constructor.get().newInstance();
      } catch (InvocationTargetException | IllegalAccessException | InstantiationException ex) {
        throw new ConfigException(String.format("Cannot instantiate config for %s with type %s", pluginId, configType.getName()), ex);
      }
    }

    return MAPPER.treeToValue(MAPPER.valueToTree(config), configType);
  }

  public List<MagpiePlugin<?>> byType(Class<? extends MagpiePlugin> clazz) {
    final var list = plugins.get(clazz);
    return list == null ? Collections.emptyList() :  Collections.unmodifiableList(list);
  }

  public Optional<MagpiePlugin<?>> byId(String id) {
    var matches = plugins.values().stream()
      .flatMap(val -> val.stream())
      .filter(plugin -> plugin.id().equals(id))
      .collect(Collectors.toList());
    assert(matches.size() <= 1);
    return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
  }
}