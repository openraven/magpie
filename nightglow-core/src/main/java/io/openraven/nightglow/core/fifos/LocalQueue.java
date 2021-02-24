package io.openraven.nightglow.core.fifos;

import io.openraven.nightglow.api.DiscoveryEnvelope;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LocalQueue implements FifoQueue, FifoDequeue{

  private final Queue<DiscoveryEnvelope> queue = new ConcurrentLinkedQueue<>();

  @Override
  public Optional<DiscoveryEnvelope> poll() throws FifoException {
    return Optional.ofNullable(queue.poll());
  }

  @Override
  public void add(DiscoveryEnvelope env) throws FifoException {
    if (!queue.add(env)) {
      throw new FifoException("Couldn't enqueue " + env.toString());
    }
  }
}
