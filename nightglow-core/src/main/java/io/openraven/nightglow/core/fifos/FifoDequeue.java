package io.openraven.nightglow.core.fifos;

import io.openraven.nightglow.api.NGEnvelope;

import java.util.Optional;

@FunctionalInterface
public interface FifoDequeue {
  Optional<NGEnvelope> poll() throws FifoException;
}
