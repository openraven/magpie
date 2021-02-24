package io.openraven.nightglow.core.fifos;

import io.openraven.nightglow.api.DiscoveryEnvelope;

@FunctionalInterface
public interface FifoQueue {
  void add(DiscoveryEnvelope env) throws FifoException;
}
