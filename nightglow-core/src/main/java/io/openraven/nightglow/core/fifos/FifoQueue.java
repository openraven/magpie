package io.openraven.nightglow.core.fifos;

import io.openraven.nightglow.api.NGEnvelope;

@FunctionalInterface
public interface FifoQueue {
  void add(NGEnvelope env) throws FifoException;
}
