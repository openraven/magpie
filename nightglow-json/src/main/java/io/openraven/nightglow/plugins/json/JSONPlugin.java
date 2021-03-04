package io.openraven.nightglow.plugins.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.openraven.nightglow.api.NGEnvelope;
import io.openraven.nightglow.api.TerminalPlugin;
import org.slf4j.Logger;

import java.io.IOException;

public class JSONPlugin implements TerminalPlugin<Void> {

  private static final String ID = "nightglow.json";
  private static final ObjectMapper MAPPER = new ObjectMapper()
    .enable(SerializationFeature.INDENT_OUTPUT)
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .findAndRegisterModules();

  private Logger logger;

  @Override
  public void accept(NGEnvelope ngEnvelope) {
    try {
//      final var obj = MAPPER.readTree(ngEnvelope.getContents());
      var output = MAPPER.writeValueAsString(ngEnvelope);  // Jackson auto-closes streams by default and we don't wish to close stdout.
      System.out.println(output);
    } catch (IOException ex) {
      logger.warn("Couldn't process envelope contents", ex);
    }
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public void init(Void unused, Logger logger) {
    this.logger = logger;
  }

  @Override
  public Class<Void> configType() {
    return null;
  }
}
