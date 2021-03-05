package io.openraven.nightglow.core.fifos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.openraven.nightglow.api.NGEnvelope;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;

import static org.junit.jupiter.api.Assertions.*;

class KafkaDequeueTest {

  private static final ObjectMapper MAPPER = new ObjectMapper()
    .enable(SerializationFeature.INDENT_OUTPUT)
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .findAndRegisterModules();

  @Test
  public void test() throws Exception {
    try(var is = KafkaDequeue.class.getResourceAsStream("/env.json")) {
      var env = MAPPER.readValue(is, NGEnvelope.class);
      assertNotNull(env);
    }
  }

}
