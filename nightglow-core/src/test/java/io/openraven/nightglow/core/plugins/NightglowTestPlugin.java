package io.openraven.nightglow.core.plugins;

import io.openraven.nightglow.api.Emitter;
import io.openraven.nightglow.api.EnumerationPlugin;
import io.openraven.nightglow.api.Session;
import org.slf4j.Logger;

public class NightglowTestPlugin implements EnumerationPlugin<NightglowTestConfig> {

  protected NightglowTestConfig config;

  @Override
  public void discover(Session session, Emitter emitter) { }

  @Override
  public String id() {
    return "nightglow.test";
  }

  @Override
  public void init(NightglowTestConfig config, Logger logger) {
    this.config = config;
  }

  @Override
  public Class<NightglowTestConfig> configType() {
    return NightglowTestConfig.class;
  }
}
