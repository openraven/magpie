/*
 * Copyright 2021 Open Raven Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
      final var queueType = QueueType.valueOf(fifoConfig.getType().toUpperCase());
      switch(queueType) {
        case LOCAL:
          var q = new LocalQueue();
          // A LocalQueue implements both Queue and Dequeue, so it must be placed in both
          // collections.
          queues.put(name, q);
          dequeues.put(name, q);
          break;
        case KAFKA:
          var qk = new KafkaQueue(fifoConfig.getProperties());
          // A LocalQueue implements both Queue and Dequeue, so it must be placed in both
          // collections.
          queues.put(name, qk);
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

        final var queueType = QueueType.valueOf(fifoConfig.getType().toUpperCase());
        switch(queueType) {
          case LOCAL:
            // Local queues are handled by the buildQueues method.
            break;
          case KAFKA:
            var dk = new KafkaDequeue(fifoConfig.getProperties());
            dequeues.put(name, dk);
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
