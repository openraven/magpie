/*
 * Copyright 2024 Open Raven Inc
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

package io.openraven.magpie.plugins.azure.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AzureUtils {

  public static final JsonNode NULL_NODE = AzureDiscoveryPlugin.MAPPER.nullNode();

  private static final Logger LOGGER = LoggerFactory.getLogger(AzureUtils.class);


  public static Map<String, Object> reflectProperties(String id, Object target, Set<Pattern> interests, Set<Pattern> nonInterests, Logger logger, ObjectMapper mapper) {
    try {
      final var map = new HashMap<String, Object>();

      for (Method method : Arrays.stream(target.getClass().getDeclaredMethods())
        .filter(m -> m.getParameterCount() == 0)            // Nothing that requires parameters
        .filter(m -> !m.getName().endsWith("Async"))        // Nothing that's async
        .filter(m -> Modifier.isPublic(m.getModifiers()))   // Public methods only
        .filter(m -> interests.stream().anyMatch(p -> p.matcher(m.getName()).matches()))    // Only methods of interest
        .filter(m -> nonInterests.stream().noneMatch(p ->p.matcher(m.getName()).matches())) // Not on the deny list
        .collect(Collectors.toList())) {
        try {
          method.setAccessible(true);
          final var res = method.invoke(target);
          if (res instanceof String || res instanceof Boolean || res instanceof Number) {
            map.put(method.getName(), res);
          } else {
            map.put(method.getName(), mapper.valueToTree(res));
          }
        } catch (Exception ex) {
          logger.info("Couldn't add method {}", method.getName(), ex);
        }
      }


      return map;
    } catch (Exception ex) {
      logger.info("Couldn't reflect properties on {}", id, ex);
      return Map.of();
    }
  }

  @SuppressWarnings("rawtypes")
  public static JsonNode update(@Nullable JsonNode payload, Map<String, Object> mappedResponsesToAdd) {

    for (Map.Entry<String, Object> responseToAdd : mappedResponsesToAdd.entrySet()) {
      ObjectNode nodeToAdd = AzureDiscoveryPlugin.MAPPER.createObjectNode();

      nodeToAdd.set(responseToAdd.getKey(),
        AzureDiscoveryPlugin.MAPPER.convertValue(responseToAdd.getValue(), JsonNode.class));

      payload = update(payload, nodeToAdd);
    }

    return payload;
  }

  public static JsonNode update(@Nullable JsonNode payload, JsonNode... nodesToAdd) {
    for (JsonNode nodeToAdd : nodesToAdd) {
      if (nodeToAdd != null) {
        try {
          if (payload != null) {
            payload = AzureDiscoveryPlugin.MAPPER.readerForUpdating(payload).readValue(nodeToAdd);
          } else {
            payload = nodeToAdd;
          }
        } catch (IOException e) {
          LOGGER.warn("Unable to add extra data {}", nodeToAdd, e);
        }
      }
    }

    return payload;
  }
}
