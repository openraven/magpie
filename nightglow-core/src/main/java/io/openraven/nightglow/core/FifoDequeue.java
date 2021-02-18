package io.openraven.nightglow.core;

import io.openraven.nightglow.api.DiscoveryEnvelope;

@FunctionalInterface
public interface FifoDequeue {
  DiscoveryEnvelope poll() throws FifoException;
}
