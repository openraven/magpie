package io.openraven.nightglow.core.plugins;

import io.openraven.nightglow.api.DiscoveryEnvelope;
import io.openraven.nightglow.api.Emitter;
import io.openraven.nightglow.api.IntermediatePlugin;
import org.slf4j.Logger;

import java.util.Set;
import java.util.regex.Pattern;

public abstract class AbstractNoopPlugin implements IntermediatePlugin<Object> {

  private static final Set<Pattern> ACCEPTS = Set.of(Pattern.compile(".*"));

  @Override
  public Set<Pattern> accepts() {
    return null;
  }

  @Override
  public void accept(DiscoveryEnvelope discoveryEnvelope, Emitter emitter) {
    var env = DiscoveryEnvelope.of(discoveryEnvelope, id(), discoveryEnvelope.getObj());
    emitter.emit(env);
  }

  @Override
  public void init(Object o, Logger logger) { }

  @Override
  public Class<Object> configType() {
    return null;
  }
}
