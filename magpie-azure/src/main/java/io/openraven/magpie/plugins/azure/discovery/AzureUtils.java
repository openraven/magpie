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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;

public class AzureUtils {

  private static final JsonNode NULL_NODE = AzureDiscoveryPlugin.MAPPER.nullNode();

  private static final Logger LOGGER = LoggerFactory.getLogger(AzureUtils.class);


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
