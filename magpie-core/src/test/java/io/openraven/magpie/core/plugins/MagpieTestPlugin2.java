package io.openraven.magpie.core.plugins;

import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.OriginPlugin;
import io.openraven.magpie.api.Session;
import org.slf4j.Logger;

public class MagpieTestPlugin2 implements OriginPlugin<Void> {

  private Object config;

  @Override
  public void discover(Session session, Emitter emitter) { }

  @Override
  public String id() {
    return "magpie.test.2";
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
