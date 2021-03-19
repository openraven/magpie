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

import io.openraven.nightglow.api.Emitter;
import io.openraven.nightglow.api.IntermediatePlugin;
import io.openraven.nightglow.api.NGEnvelope;
import org.slf4j.Logger;

public class IdentityPlugin implements IntermediatePlugin<Void> {

  private static final String ID = "nightglow.identity";

  private Logger logger;

  @Override
  public void accept(NGEnvelope ngEnvelope, Emitter emitter) {
    logger.debug("Consumed {}", ngEnvelope.getContents());
    emitter.emit(NGEnvelope.of(ngEnvelope, ID, ngEnvelope.getContents()));
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public void init(Void unused, Logger logger) {
    this.logger = logger;
  }

  @Override
  public Class<Void> configType() {
    return null;
  }
}
