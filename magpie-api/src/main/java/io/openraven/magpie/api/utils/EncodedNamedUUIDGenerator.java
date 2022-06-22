/*-
 * #%L
 * magpie-api
 * %%
 * Copyright (C) 2021 Open Raven Inc
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.openraven.magpie.api.utils;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

/**
 * Inspired by https://github.com/elastic/elasticsearch/blob/c610ef2955d2d147503968434879dfaf6aa24177/server/src/main/java/org/elasticsearch/common/TimeBasedUUIDGenerator.java
 */
public class EncodedNamedUUIDGenerator {

  public static String getEncodedNamedUUID(String target) {
    if (target.isEmpty()) {
      throw new IllegalStateException("Target for Encoded Named UUID is empty.");
    }

    byte[] targetBytes = target.toLowerCase().getBytes();
    UUID uuid = UUID.nameUUIDFromBytes(targetBytes);

    ByteBuffer uuidBytes = ByteBuffer.wrap(new byte[16]);
    uuidBytes.putLong(uuid.getMostSignificantBits());
    uuidBytes.putLong(uuid.getLeastSignificantBits());

    return Base64.getUrlEncoder().withoutPadding().encodeToString(uuidBytes.array());
  }

}
