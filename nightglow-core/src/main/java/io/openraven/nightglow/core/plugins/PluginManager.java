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

package io.openraven.nightglow.core.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.nightglow.api.EnumerationPlugin;
import io.openraven.nightglow.api.IntermediatePlugin;
import io.openraven.nightglow.api.NightglowPlugin;
import io.openraven.nightglow.api.TerminalPlugin;
import io.openraven.nightglow.core.config.ConfigException;
import io.openraven.nightglow.core.config.NightglowConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
  private static final List<Class<? extends NightglowPlugin>> PLUGIN_CLASSES = List.of(EnumerationPlugin.class, IntermediatePlugin.class, TerminalPlugin.class);

  private final NightglowConfig config;

  private final Map<Class<? extends NightglowPlugin>, List<NightglowPlugin<?>>> plugins = new HashMap<>();

  public PluginManager(NightglowConfig config) {
    this.config = config;
    loadPlugins();
  }

  private void loadPlugins() {
    final var pluginsFound = new AtomicLong();
    PLUGIN_CLASSES.forEach(c -> {
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
            final var pluginConfig = buildPluginConfig(configType, pluginConfigParent.getConfig());

            plugin.init(pluginConfig, LoggerFactory.getLogger(plugin.getClass()));
            var pluginList = plugins.getOrDefault(c, new ArrayList<>());
            pluginList.add(plugin);
            plugins.put(c, pluginList);
            LOGGER.debug("Loaded {}", plugin.id());
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

  private Object buildPluginConfig(Class<?> configType, Object config) throws JsonProcessingException {
    if (config == null) {
      return null;
    }
    return MAPPER.treeToValue(MAPPER.valueToTree(config), configType);
  }

  public List<NightglowPlugin<?>> byType(Class<? extends NightglowPlugin> clazz) {
    final var list = plugins.get(clazz);
    return list == null ? Collections.emptyList() :  Collections.unmodifiableList(list);
  }

  public Optional<NightglowPlugin<?>> byId(String id) {
    var matches = plugins.values().stream()
      .flatMap(val -> val.stream())
      .filter(plugin -> plugin.id().equals(id))
      .collect(Collectors.toList());
    assert(matches.size() <= 1);
    return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
  }
}
