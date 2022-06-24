/*-
 * #%L
 * Magpie API
 * %%
 * Copyright (C) 2021 Open Raven Inc
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
package io.openraven.magpie.data.aws;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;

public class Envelope {
  @JsonIgnore
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public String integration;

  public String discoverySession;

  public String ts;

  public AWSResource contents;

  static private class DiscoveryMeta {
    public String discoverySessionId;
    public String updatedIso;

    public DiscoveryMeta(final String discoverySessionId) {
      this.discoverySessionId = discoverySessionId;
      this.updatedIso = Instant.now().toString();
    }
  }

  public Envelope() {

  }

  public Envelope(final String integration, final String discoverySession, final String ts, final AWSResource contents) {
    this.integration = integration;
    this.discoverySession = discoverySession;
    this.ts = ts;
    this.contents = contents;
    // TODO: one day we can deprecate placing discoverySessionId in the root of the document instead of in the discoveryMeta object.
    // Keeping this here for potential backwards compatibility issues
    this.contents.discoverySessionId = discoverySession;
    DiscoveryMeta metaData = new DiscoveryMeta(discoverySession);
    this.contents.discoveryMeta = MAPPER.valueToTree(metaData);
  }

  public String getIntegration() {
    return integration;
  }

  @SuppressWarnings("unused")
  public void setIntegration(String integration) {
    this.integration = integration;
  }

  public String getDiscoverySession() {
    return discoverySession;
  }

  public void setDiscoverySession(String discoverySession) {
    this.discoverySession = discoverySession;
  }

  @SuppressWarnings("unused")
  public String getTs() {
    return ts;
  }

  @SuppressWarnings("unused")
  public void setTs(String ts) {
    this.ts = ts;
  }

  @SuppressWarnings("unused")
  public AWSResource getContents() {
    return contents;
  }

  public void setContents(AWSResource contents) {
    this.contents = contents;
  }

}
