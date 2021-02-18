package io.openraven.nightglow.core.plugins;

import io.openraven.nightglow.api.QueryPlugin;

public class NoopQueryPlugin extends AbstractNoopPlugin implements QueryPlugin<Object> {

  private static final String ID = "nightglow.noopquery";

  @Override
  public String id() {
    return ID;
  }

}
