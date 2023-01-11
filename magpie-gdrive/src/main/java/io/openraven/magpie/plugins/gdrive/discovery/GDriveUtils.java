package io.openraven.magpie.plugins.gdrive.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.appengine.repackaged.com.google.common.base.Pair;
import com.google.appengine.repackaged.com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GDriveUtils {
  private static final Logger logger = LoggerFactory.getLogger(GDriveUtils.class);
  private static final ObjectMapper mapper = createObjectMapper();

  public  static ObjectMapper createObjectMapper() {
    return  new ObjectMapper()
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .findAndRegisterModules();
  }

  public static JsonNode asJsonNode(Object object) {
    String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(object);

    try {
      return mapper.readValue(jsonString, JsonNode.class);
    } catch (JsonProcessingException e) {
      logger.error("Unexpected JsonProcessingException this shouldn't happen at all");
    }

    return mapper.createObjectNode();
  }

  public static void update(JsonNode payload, Pair<String, Object> objectPair) {
    ObjectNode o = (ObjectNode) payload;
    o.set(objectPair.first, GDriveUtils.asJsonNode(objectPair.second));
  }
}
