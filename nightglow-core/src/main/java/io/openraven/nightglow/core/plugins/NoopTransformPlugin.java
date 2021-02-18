package io.openraven.nightglow.core.plugins;

import io.openraven.nightglow.api.QueryPlugin;

public class NoopTransformPlugin extends AbstractNoopPlugin implements QueryPlugin<Object> {
  
  private static final String ID = "nightglow.nooptransform";

  @Override
  public String id() {
    return ID;
  }

}
