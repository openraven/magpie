/*-
 * #%L
 * Magpie API
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
 * A plugin that represents the terminal operation in the Magpie framework. TerminalPlugins will receive discovered
 * assetts via {@link #accept(MagpieEnvelope)} but are responsible for exporting the data to another system or datastore. This
 * may be an output stream (stdout), static file, an RDBMS, MQ, or other.
 * @param <T> The configuration object class to be passed into the {@link MagpiePlugin#init(Object config, Logger logger)}  init} method.  This is a developer-defined
 *           Jackson-serializable POJO and should be distributed with the plugin.
 * @author Jason Nichols (jason@openraven.com)
 */
public interface TerminalPlugin<T> extends MagpiePlugin<T> {
  /**
   * Accept an incoming discovered asset.
   * @param env A container for the asset discovered, in an arbitrary format.  JSON is favored by all Magpie-maintained
   *            plugins.
   */
  void accept(MagpieEnvelope env);
}
