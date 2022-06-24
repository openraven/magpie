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
 * IntermediatePlugin implementations provide the glue between Origin (discovery) plugins and Terminal (output) plugins.  They may
 * perform more in-depth discovery on assets or do schema transformations to support specific downstream schemas.  They
 * both accept incoming {@link MagpieEnvelope envelopes} and output via the provided {@link Emitter}. Plugins need not emit anything (acting as a filter)
 * or may emit multiple downstream envelopes for a single received envelope.
 * @param <T> The configuration object class to be passed into the {@link MagpiePlugin#init(Object config, Logger logger)}  init} method.  This is a developer-defined
 *           Jackson-serializable POJO and should be distributed with the plugin.
 * @author Jason Nichols (jason@openraven.com)
 */
public interface IntermediatePlugin<T> extends MagpiePlugin<T> {
  /**
   * Accept incoming assets for processing and emit downstream.
   * @param env The {@link MagpieEnvelope} provided by a higher layer plugin.
   * @param emitter The {@link Emitter} to pass processed envelopes downstream.
   */
  void accept(MagpieEnvelope env, Emitter emitter);
}
