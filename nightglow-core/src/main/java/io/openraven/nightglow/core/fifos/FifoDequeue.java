package io.openraven.nightglow.core.fifos;

import io.openraven.nightglow.api.DiscoveryEnvelope;

import java.util.Optional;

@FunctionalInterface
public interface FifoDequeue {
  Optional<DiscoveryEnvelope> poll() throws FifoException;
}
