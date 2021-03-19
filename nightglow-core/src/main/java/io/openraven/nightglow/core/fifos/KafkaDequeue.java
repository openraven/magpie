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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.openraven.nightglow.api.NGEnvelope;
import io.openraven.nightglow.core.config.ConfigException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class KafkaDequeue implements FifoDequeue{

  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaDequeue.class);

  private static final ObjectMapper MAPPER = new ObjectMapper()
    .enable(SerializationFeature.INDENT_OUTPUT)
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .findAndRegisterModules();

  // How long to poll before returning (in ms).
  private static final long POLL_TIMEOUT = 100L;

  private static final Map<String, Object> DEFAULT_PROPERTIES = Map.of(
    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
    ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1  // We need to adjust the FifoDequeue API to allow more than 1.
  );

  private final Consumer<String, String> consumer;

  public KafkaDequeue(Map<String, Object> properties) {
    var t = properties.remove("topic");
    if (Objects.isNull(t)) {
      throw new ConfigException("Kafka 'topic' value must be set under properties");
    }
    var props = new HashMap<String, Object>();
    props.putAll(DEFAULT_PROPERTIES);
    props.putAll(properties);

    consumer = new KafkaConsumer<>(props);
    consumer.subscribe(List.of(t.toString()));
  }

  @Override
  public Optional<NGEnvelope> poll() throws FifoException {
    var records = consumer.poll(Duration.ofMillis(POLL_TIMEOUT));
    if (!records.isEmpty()) {
      for (var r : records) {
        try {
          return Optional.of(MAPPER.readValue(r.value(), NGEnvelope.class));
        } catch (JsonProcessingException ex) {
          throw new FifoException("Couldn't deserialize envelope", ex);
        }
      }
    }

    return Optional.empty();
  }
}
