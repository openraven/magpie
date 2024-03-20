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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.appengine.repackaged.com.google.common.base.Pair;
import com.google.appengine.repackaged.org.joda.time.LocalDate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class GCPUtils {


  private static class DurationSerializer implements JsonSerializer<Duration> {
    @Override
    public JsonElement serialize(Duration duration, Type type, JsonSerializationContext jsonSerializationContext) {
      return new JsonPrimitive(duration.toString());
    }
  }

  static class LocalDateTimeSerializer implements JsonSerializer <LocalDateTime> {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss");

    @Override
    public JsonElement serialize(LocalDateTime localDateTime, Type srcType, JsonSerializationContext context) {
      return new JsonPrimitive(formatter.format(localDateTime));
    }
  }

  static class OffsetDateTimeSerializer implements JsonSerializer <OffsetDateTime> {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss");

    @Override
    public JsonElement serialize(OffsetDateTime offsetDateTime, Type srcType, JsonSerializationContext context) {
      return new JsonPrimitive(formatter.format(offsetDateTime));
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(GCPUtils.class);
  private static final ObjectMapper mapper = createObjectMapper();

  private static final Gson GSON = new GsonBuilder()
    .setPrettyPrinting()
    .registerTypeAdapter(OffsetDateTime.class, (JsonDeserializer<OffsetDateTime>) (json, type, context) -> OffsetDateTime.parse(json.getAsString()))
    .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, context) -> LocalDateTime.parse(json.getAsString()))
    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer())
    .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeSerializer())
    .registerTypeAdapter(Duration.class, new DurationSerializer())
    .create();

  public  static ObjectMapper createObjectMapper() {
    return  new ObjectMapper()
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
      .findAndRegisterModules();
  }

  public static JsonNode asJsonNode(Object object) {
    String jsonString = GSON.toJson(object);

    try {
      return mapper.readValue(jsonString, JsonNode.class);
    } catch (JsonProcessingException e) {
      logger.error("Unexpected JsonProcessingException this shouldn't happen at all");
    }

    return mapper.createObjectNode();
  }
  public static JsonNode asJsonNode(AbstractMessage msg) {

    try {
      String jsonString = JsonFormat.printer().print(msg);
      return mapper.readValue(jsonString, JsonNode.class);
    } catch (JsonProcessingException | InvalidProtocolBufferException e) {
      logger.error("Unexpected JsonProcessingException this shouldn't happen at all");
    }

    return mapper.createObjectNode();
  }

  public static void update(JsonNode payload, Pair<String, Object> objectPair) {
    ObjectNode o = (ObjectNode) payload;
    o.set(objectPair.first, GCPUtils.asJsonNode(objectPair.second));
  }

  public static Instant protobufTimestampToInstant(com.google.protobuf.Timestamp timestamp) {
    if (timestamp == null) {
      return null;
    }
    return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
  }

  public static String selfLinkToAssetId(String selfLink) {
    return selfLink
      .replaceFirst("https:", "")
      .replaceFirst("/v1", "");
  }

  public static String assetNameToAssetId(String service, String projectId, String assetType, String assetName) {
    return String.format("//%s.googleapis.com/projects/%s/%s/%s", service, projectId, assetType, assetName);
  }
}
