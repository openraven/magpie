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

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * OriginPlugin kicks off the discovery process and emits discovered assets via the supplied Emitter.   {@link #discover(Session, Emitter) discover}
 * is invoked once per session. An OriginPlugin may emit any number of assets by calling {@link Emitter#emit(MagpieEnvelope) Emitter.emit}
 * zero or more times.
 * @param <T> The configuration object class to be passed into the {@link MagpiePlugin#init(Object config, Logger logger)}  init} method.  This is a developer-defined
 *           Jackson-serializable POJO and should be distributed with the plugin.
 * @author Jason Nichols (jason@openraven.com)
 */
public interface OriginPlugin<T> extends MagpiePlugin<T> {
  /**
   * Kicks off the a discovery session.
   * @param session The unique {@link Session} for this discovery session.
   * @param emitter The emitter used to pass discovered assets into the discovery pipeline.
   */
  void discover(Session session, Emitter emitter) throws GeneralSecurityException, IOException;
}
