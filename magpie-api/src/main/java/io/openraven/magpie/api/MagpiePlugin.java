/*-
 * #%L
 * magpie-api
 * %%
 * Copyright (C) 2021 Open Raven Inc
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package io.openraven.magpie.api;

import org.slf4j.Logger;

/**
 * Parent interface for Magpie plugins.  This interface is not meant to be directly implemented but instead  developers
 * should implement one of {@link OriginPlugin}, {@link IntermediatePlugin}, or {@link TerminalPlugin}.
 * @param <T> The configuration object class to be passed into the {@link MagpiePlugin#init(Object config, Logger logger)}  init} method.  This is a developer-defined
 *           Jackson-serializable POJO and should be distributed with the plugin.
 * @author Jason Nichols (jason@openraven.com)
 */
public interface MagpiePlugin<T> {
  /**
   * The unique ID of the plugin. The name should follow the form &lt;creator&gt;.&lt;service&gt;.&lt;plugintype&gt;, for
   * example "magpie.aws.discovery".  Plugin type should be one
   * <ol>
   *   <li>discovery</li>
   *   <li>transform</li>
   *   <li>output</li>
   * </ol>
   * Or other types that may emerge from usage.
   * @return The unique ID of the plugin
   */
  String id();

  /**
   * Initialize the plugin with a configuration and Logger interface. Plugins should <em>NOT</em> instantiate their own
   * logger and should instead use the provided one.  Printing to stdout may break plugins or pipes relying on output
   * in a certain schema.  By default loggers print to stderr.
   * @param config A configuration object for this plugin. This is a developer-defined Jackson-serializable POJO and should be distributed with the plugin.
   * @param logger An SLF4J logger instance to be used as needed.
   */
  void init(T config, Logger logger);

  default void shutdown() {}

  /**
   * The class of the configuration object passed to {@link #init(Object, Logger)}
   * @return
   */
  Class<T> configType();
}
