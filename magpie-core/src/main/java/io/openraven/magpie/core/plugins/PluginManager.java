/*
 * Copyright 2021 Open Raven Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openraven.magpie.core.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.MagpiePlugin;
import io.openraven.magpie.core.config.ConfigException;
import io.openraven.magpie.core.config.MagpieConfig;
import org.python.google.common.collect.Lists;
import org.python.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class PluginManager {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final Logger LOGGER = LoggerFactory.getLogger(PluginManager.class);

  private final MagpieConfig config;

  private final Map<Class<? extends MagpiePlugin>, List<MagpiePlugin<?>>> plugins = new HashMap<>();

  public PluginManager(MagpieConfig config) {
    this.config = config;
  }

  public void loadPlugins(List<Class<? extends MagpiePlugin>> pluginClasses) {
    final var pluginsFound = new AtomicLong();
    pluginClasses.forEach(c -> {
      final var loader = ServiceLoader.load(c);
      var count = loader.stream().count();
      LOGGER.debug("Found {} {} plugin(s) via the classpath", c.getSimpleName(), count);
      pluginsFound.addAndGet(count);
      loader.stream().forEach(svc -> {
        final var plugin = svc.get();
        try {
          final var configType = plugin.configType();
          final var pluginConfigParent = config.getPlugins().get(plugin.id());
          if (pluginConfigParent == null) {
            LOGGER.debug("No configuration found for {}, ignoring.", plugin.id());
          } else if (!pluginConfigParent.isEnabled()) {
            LOGGER.debug("{} found but is disabled via config. Ignoring}", plugin.id());
          } else {
            final var pluginConfig = buildPluginConfig(plugin.id(), configType, pluginConfigParent.getConfig());

            plugin.init(pluginConfig, LoggerFactory.getLogger(plugin.getClass()));
            var pluginList = plugins.getOrDefault(c, new ArrayList<>());
            pluginList.add(plugin);
            plugins.put(c, pluginList);
            LOGGER.debug("Loaded {}", plugin.id());
          }
        } catch (Exception ex) {
          throw new PluginLoaderException(ex);
        }
      });
    });

    if (pluginsFound.get() == 0) {
      throw new ConfigException("No plugins found");
    }
  }

  public static Object buildPluginConfig(String pluginId, Class<?> configType, Object config) throws JsonProcessingException {

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

      // TODO: Investigate why the GCP Plugin doesn't require this line but AWS Discovery and the Persistence Plugin do.
      final var pluginList = Sets.newHashSet("magpie.aws.discovery", "magpie.persist");
      if (pluginList.contains(pluginId))  {
        return MAPPER.treeToValue(MAPPER.valueToTree(config), configType);
      }

      return config;
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
