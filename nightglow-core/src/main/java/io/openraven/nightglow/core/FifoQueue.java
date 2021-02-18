package io.openraven.nightglow.core;

import io.openraven.nightglow.api.DiscoveryEnvelope;

@FunctionalInterface
public interface FifoQueue {
  void add(DiscoveryEnvelope env) throws FifoException;
}
