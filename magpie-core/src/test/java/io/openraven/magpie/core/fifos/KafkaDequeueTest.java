package io.openraven.magpie.core.fifos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.openraven.magpie.api.MagpieEnvelope;
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
      var env = MAPPER.readValue(is, MagpieEnvelope.class);
      assertNotNull(env);
    }
  }

}
