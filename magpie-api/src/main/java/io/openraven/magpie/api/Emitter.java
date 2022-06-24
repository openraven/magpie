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

/**
 * <p>Emitter represents an abstract method of emitting discovery envelopes through the Magpie pipeline.  Plugin
 * implementation need not be aware of how envelopes traverse the pipeline or where the next destination is.</p>
 *
 * <p>A plugin may emit 0+ envelopes for each discovery/processing invocation.  It is not recommended that
 * plugins attempt to collect multiple envelopes from upstream as Magpie makes no guarantee reception of
 * all envelopes by a single plugin when multiple plugins exist on a single layer.</p>
 */
@FunctionalInterface
public interface Emitter {

  /**
   * Emit an envelope into the pipeline.  A plugin may emit an arbitrary number of envelopes.
   * @param env The envelope being emitted into the pipeline.
   */
  void emit(MagpieEnvelope env);
}
