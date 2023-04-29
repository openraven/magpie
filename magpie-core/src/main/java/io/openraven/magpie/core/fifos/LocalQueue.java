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

package io.openraven.magpie.core.fifos;

import io.openraven.magpie.api.MagpieEnvelope;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LocalQueue implements FifoQueue, FifoDequeue{

  private final LinkedBlockingQueue<MagpieEnvelope> queue = new LinkedBlockingQueue<>();

  @Override
  public Optional<MagpieEnvelope> poll() throws FifoException {
    return Optional.ofNullable(queue.poll());
  }

  public List<MagpieEnvelope> drain() throws FifoException {
    var list = new LinkedList<MagpieEnvelope>();
    queue.drainTo(list);
    return list;
  }

  @Override
  public void add(MagpieEnvelope env) throws FifoException {
    if (!queue.add(env)) {
      throw new FifoException("Couldn't enqueue " + env.toString());
    }
  }
}
