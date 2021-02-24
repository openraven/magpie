package io.openraven.nightglow.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.openraven.nightglow.core.config.NightglowConfig;
import io.openraven.nightglow.core.fifos.FifoManager;
import io.openraven.nightglow.core.fifos.LocalQueue;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class FifoManagerTest {

  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  @Test
  public void testSimpleConfig() throws Exception {
    try(InputStream is = FifoManagerTest.class.getResourceAsStream("/fifoconfig/simple.yaml")) {
      var config = MAPPER.readValue(is, NightglowConfig.class);

      assertEquals(3, config.getLayers().size());
      assertTrue(config.getLayers().containsKey("discovery"));
      assertTrue(config.getLayers().containsKey("transform"));
      assertTrue(config.getLayers().containsKey("output"));
      assertFalse(config.getLayers().containsKey("test"));

      var fifoManager = new FifoManager(config);
      assertTrue(fifoManager.getDequeue("default") instanceof LocalQueue);
      assertTrue(fifoManager.getQueue("default") instanceof LocalQueue);
    }
  }
}

