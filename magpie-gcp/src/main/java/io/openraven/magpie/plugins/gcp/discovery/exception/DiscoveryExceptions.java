/*
 * Copyright 2023 Open Raven Inc
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
package io.openraven.magpie.plugins.gcp.discovery.exception;

import io.grpc.StatusRuntimeException;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscoveryExceptions {

  private static final Logger logger = LoggerFactory.getLogger(DiscoveryExceptions.class);

  static public void onDiscoveryException(String resourceType, Exception ex) {
    logger.error("{} - Exception , with error {}", resourceType, ex.getMessage());

    //
    // We do not want to sent Sentry events for exceptions caused by StatusRuntimeExceptions indicating the root
    // cause is a disabled API.
    // See PROD-7671
    //
    if (ex instanceof StatusRuntimeException && ex.getMessage() != null && ex.getMessage().contains("API has not been used")) {
      return;
    }

    final var event = new SentryEvent();
    event.setLevel(SentryLevel.WARNING);
    final var message = new Message();
    message.setMessage(resourceType + ":" + ex.getMessage());
    event.setMessage(message);
    Sentry.captureEvent(event);
  }
}
