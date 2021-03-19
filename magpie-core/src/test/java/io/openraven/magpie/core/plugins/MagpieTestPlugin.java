package io.openraven.magpie.core.plugins;

import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.OriginPlugin;
import io.openraven.magpie.api.Session;
import org.slf4j.Logger;

public class MagpieTestPlugin implements OriginPlugin<MagpieTestConfig> {

  protected MagpieTestConfig config;

  @Override
  public void discover(Session session, Emitter emitter) { }

  @Override
  public String id() {
    return "magpie.test";
  }

  @Override
  public void init(MagpieTestConfig config, Logger logger) {
    this.config = config;
  }

  @Override
  public Class<MagpieTestConfig> configType() {
    return MagpieTestConfig.class;
  }
}
