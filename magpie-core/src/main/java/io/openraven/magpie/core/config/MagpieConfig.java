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

package io.openraven.magpie.core.config;

import java.util.HashMap;
import java.util.Map;

public class MagpieConfig {

  private Map<String, LayerConfig> layers = new HashMap<>();
  private Map<String, FifoConfig> fifos = new HashMap<>();
  private Map<String, PluginConfig> plugins = new HashMap<>();

  public Map<String, FifoConfig> getFifos() {
    return fifos;
  }

  public void setFifos(Map<String, FifoConfig> fifos) {
    this.fifos = fifos;
  }

  public Map<String, LayerConfig> getLayers() {
    return layers;
  }

  public void setLayers(Map<String, LayerConfig> layers) {
    this.layers = layers;
  }

  public Map<String, PluginConfig> getPlugins() {
    return plugins;
  }

  public void setPlugins(Map<String, PluginConfig> plugins) {
    this.plugins = plugins;
  }
}
