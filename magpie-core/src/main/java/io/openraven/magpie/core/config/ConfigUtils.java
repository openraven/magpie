package io.openraven.magpie.core.config;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class ConfigUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigUtils.class);

  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  private static final Pattern ENV_VAR_OVERRIDE_PATTERN = Pattern.compile("MAGPIE_CONFIG.*");

  private static Map<JsonPointer, JsonNode> toPointers(ObjectNode node) {
    var map = new LinkedHashMap<JsonPointer, JsonNode>();
    var iter = node.fields();
    while (iter.hasNext()) {
      var entry  = iter.next();
      map.put(JsonPointer.compile(entry.getKey()), entry.getValue());
    }
    return map;
  }

  /**
   * @param env Map of key-value pairs, where any keys that match ENV_VAR_OVERRIDE_PATTERN are expected to have values
   *            that map to JSON arrays that map JSON Pointers to JSON values/objects.
   */
  static Map<JsonPointer, JsonNode> envOverrides(Map<String, String> env) {
    var map = new LinkedHashMap<JsonPointer, JsonNode>();

    env.keySet().stream()
      .filter(e -> ENV_VAR_OVERRIDE_PATTERN.matcher(e).matches())
      .sorted()
      .forEach((e -> {
        JsonNode value = null;
        try {
          value = MAPPER.readTree(env.get(e));
        } catch (JsonProcessingException ex) {
          throw new ConfigException("Couldn't parse environmental variable " + e, ex);
        }
        if (value.isArray()) {
          var node = (ArrayNode)value;
          node.forEach(child -> {
            if (child.isObject()) {
              map.putAll(toPointers((ObjectNode)child));
            } else {
              throw new ConfigException(String.format("Couldn't parse environment override.  Values for %s must be a JSON array of the form [{<jsonPointer>: <value>}]", e));
            }
          });
        } else if (value.isObject()) {
          map.putAll(toPointers((ObjectNode)value));
        } else {
          throw new ConfigException(String.format("Couldn't parse environment override.  Value for %s must be a JSON array or JSON object.", e));
        }
      }));

    return map;
  }

  public static MagpieConfig merge(MagpieConfig config, Map<String, String> env) throws JsonProcessingException {

    final var overrides = envOverrides(env);

    ObjectNode configNode = MAPPER.valueToTree(config);

    overrides.forEach((key, value) -> {
      final var parentName = key.head();
      final var overriddenNode = configNode.at(key);
      if (overriddenNode.isMissingNode()) {
        LOGGER.warn("Cannot override {}, no configuration with that path exists", key);
      } else {
        final var leafName = key.last().toString().replaceFirst("/", "");
        final var parentNode = (ObjectNode) configNode.at(parentName);
        LOGGER.debug("Replacing {} with value {}", key, value.toPrettyString());
        parentNode.replace(leafName, value);
      }
    });
    return MAPPER.treeToValue(configNode, MagpieConfig.class);
  }

  private ConfigUtils(){}
}
