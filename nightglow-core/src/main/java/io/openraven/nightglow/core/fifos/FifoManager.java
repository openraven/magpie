package io.openraven.nightglow.core.fifos;

import io.openraven.nightglow.core.config.ConfigException;
import io.openraven.nightglow.core.config.LayerConfig;
import io.openraven.nightglow.core.config.NightglowConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class FifoManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(FifoManager.class);

  private final Map<String, FifoQueue> queues = new HashMap<>();
  private final Map<String, FifoDequeue> dequeues = new HashMap<>();
  private final NightglowConfig config;

  public FifoManager(NightglowConfig config) {
    this.config = config;
    List<String> buildQueues = config.getLayers().values().stream()
      .map(LayerConfig::getQueue)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());

    List<String> buildDequeues = config.getLayers().values().stream()
      .map(LayerConfig::getDequeue)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());

    buildQueues(buildQueues);
    buildDequeues(buildDequeues);
  }

  private void buildQueues (List<String> buildQueues) {
    buildQueues.forEach(name -> {
      if (queues.containsKey(name)) {
        throw new ConfigException("Duplicate queue name: " + name);
      }

      var fifoConfig = config.getFifos().get(name);
      if (fifoConfig == null) {
        throw new ConfigException("No fifo definition found for " + name);
      }
      final var queueType = fifoConfig.getType();
      switch(queueType.toLowerCase()) {
        case "local":
          var q = new LocalQueue();
          // A LocalQueue implements both Queue and Dequeue, so it must be placed in both
          // collections.
          queues.put(name, q);
          dequeues.put(name, q);
          break;
        default:
          throw new ConfigException("Invalid queue type: " + queueType);
      }
      LOGGER.debug("Created {}:{}", name,queueType);
    });
  }

  private void buildDequeues(List<String> buildDequeues) {
    buildDequeues.stream()
      .filter(name -> !dequeues.containsKey(name))
      .forEach(name -> {

        var fifoConfig = config.getFifos().get(name);
        if (fifoConfig == null) {
          throw new ConfigException("No fifo definition found for " + name);
        }

        final var queueType = fifoConfig.getType();
        switch(queueType.toLowerCase()) {
          case "local":
            // Local queues are handled by the buildQueues method.
            break;
          default:
            throw new ConfigException("Invalid queue type: " + queueType);
        }
      });
  }


  public FifoQueue getQueue(String name) {
    return queues.get(name);
  }

  public FifoDequeue getDequeue(String name) {
    return dequeues.get(name);
  }
}
