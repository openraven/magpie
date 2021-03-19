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

import io.openraven.nightglow.api.NGEnvelope;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LocalQueue implements FifoQueue, FifoDequeue{

  private final Queue<NGEnvelope> queue = new ConcurrentLinkedQueue<>();

  @Override
  public Optional<NGEnvelope> poll() throws FifoException {
    return Optional.ofNullable(queue.poll());
  }

  @Override
  public void add(NGEnvelope env) throws FifoException {
    if (!queue.add(env)) {
      throw new FifoException("Couldn't enqueue " + env.toString());
    }
  }
}
