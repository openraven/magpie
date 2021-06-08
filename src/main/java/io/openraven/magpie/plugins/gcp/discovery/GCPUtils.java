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

package io.openraven.magpie.plugins.gcp.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.repackaged.com.google.gson.GsonBuilder;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GCPUtils {
  private static final Logger logger = LoggerFactory.getLogger(AWSUtils.class);

  public static JsonNode asJsonNode(Object object, ObjectMapper mapper) {
    String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(object);

    try {
      return mapper.readValue(jsonString, JsonNode.class);
    } catch (JsonProcessingException e) {
      logger.error("Unexpected JsonProcessingException this shouldn't happen at all");
    }

    return mapper.createObjectNode();
  }
}
