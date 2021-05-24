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

package io.openraven.magpie.plugins.json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.api.TerminalPlugin;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class JSONPlugin implements TerminalPlugin<Void> {

  private final Object SYNC = new Object();

  private static final String ID = "magpie.json";
  private static final ObjectMapper MAPPER = new ObjectMapper()
    .enable(SerializationFeature.INDENT_OUTPUT)
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .findAndRegisterModules();

  private Logger logger;
  private JsonGenerator generator;

  @Override
  public void accept(MagpieEnvelope env) {
    synchronized (SYNC) {
      try {
        generator.writeObject(env.getContents());
      } catch (IOException ex) {
        logger.warn("Couldn't process envelope contents", ex);
      }
    }
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public void init(Void unused, Logger logger) {
    this.logger = logger;
    try {
      generator = new JsonFactory().createGenerator(System.out, JsonEncoding.UTF8).setCodec(MAPPER);
      generator.writeStartArray();
    } catch (IOException ex) {
      throw new RuntimeException("JSON generator error", ex);
    }
  }

  @Override
  public void shutdown() {
    synchronized (SYNC) {
      try {
        generator.writeEndArray();
        generator.close();
      } catch (IOException ex) {
        throw new RuntimeException("JSON generator error", ex);
      }
    }
  }

  @Override
  public Class<Void> configType() {
    return null;
  }
}
