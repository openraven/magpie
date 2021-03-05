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
