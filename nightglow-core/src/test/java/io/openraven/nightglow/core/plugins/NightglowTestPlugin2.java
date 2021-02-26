package io.openraven.nightglow.core.plugins;

import io.openraven.nightglow.api.Emitter;
import io.openraven.nightglow.api.EnumerationPlugin;
import io.openraven.nightglow.api.Session;
import org.slf4j.Logger;

public class NightglowTestPlugin2 implements EnumerationPlugin<Void> {

  private Object config;

  @Override
  public void discover(Session session, Emitter emitter) { }

  @Override
  public String id() {
    return "nightglow.test.2";
  }

  @Override
  public void init(Void v, Logger logger) {
    config = v;
  }

  @Override
  public Class<Void> configType() {
    return Void.class;
  }
}
