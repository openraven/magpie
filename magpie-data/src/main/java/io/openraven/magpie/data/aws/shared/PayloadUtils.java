/*-
 * #%L
 * Magpie API
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
package io.openraven.magpie.data.aws.shared;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openraven.magpie.data.utils.JacksonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PayloadUtils {

  private static final Logger LOG = LoggerFactory.getLogger(PayloadUtils.class);
  private static final ObjectMapper objectMapper = JacksonMapper.getJacksonMapper();
  private static final JsonNode NULL_NODE = objectMapper.nullNode();

  /**
   * @param resp will be provided the output from calling {@code fn}, or @param noresp a {@code NullNode} in the 403 or 404 case
   * @throws SdkServiceException if it is not one of the 403 or 404 status codes
   */
  public static <R> void getAwsResponse(Supplier<R> fn, Consumer<R> resp, Consumer<JsonNode> noresp) throws SdkClientException, SdkServiceException {
    try {
      R ret = fn.get();
      resp.accept(ret);
    }
    catch (SdkServiceException ex) {
      if (ex.statusCode() >= 400 && ex.statusCode() < 500) {
        noresp.accept(NULL_NODE);
      }
      else {
        throw ex;
      }
    }
  }


  @SuppressWarnings("rawtypes")
  public static JsonNode update(@Nullable JsonNode payload, ToCopyableBuilder... responsesToAdd) {
    for (ToCopyableBuilder responseToAdd : responsesToAdd) {
      if (responseToAdd != null) {
        JsonNode jsonNode = objectMapper.convertValue(responseToAdd.toBuilder(), JsonNode.class);
        payload = update(payload, jsonNode);
      }
    }
    return payload;
  }

  @SuppressWarnings("rawtypes")
  public static JsonNode update(@Nullable JsonNode payload,
      Map<String, Object> mappedResponsesToAdd) {

    for (Entry<String, Object> responseToAdd : mappedResponsesToAdd.entrySet()) {
      ObjectNode nodeToAdd = objectMapper.createObjectNode();

      if (responseToAdd.getValue() instanceof ToCopyableBuilder) {
        nodeToAdd.set(responseToAdd.getKey(),
            objectMapper.convertValue(((ToCopyableBuilder) responseToAdd.getValue()).toBuilder(),
                JsonNode.class));
      } else {
        nodeToAdd.set(responseToAdd.getKey(),
            objectMapper.convertValue(responseToAdd.getValue(), JsonNode.class));
      }

      payload = update(payload, nodeToAdd);
    }

    return payload;
  }

  public static JsonNode update(@Nullable JsonNode payload, JsonNode... nodesToAdd) {
    for (JsonNode nodeToAdd : nodesToAdd) {
      if (nodeToAdd != null) {
        try {
          if (payload != null) {
            payload = objectMapper.readerForUpdating(payload).readValue(nodeToAdd);
          } else {
            payload = nodeToAdd;
          }
        } catch (IOException e) {
          LOG.warn("Unable to add extra data {}", nodeToAdd, e);
        }
      }
    }

    return payload;
  }

  @SuppressWarnings("rawtypes")
  public static JsonNode update(ToCopyableBuilder... responsesToAdd) {
    return update(null, responsesToAdd);
  }
}
