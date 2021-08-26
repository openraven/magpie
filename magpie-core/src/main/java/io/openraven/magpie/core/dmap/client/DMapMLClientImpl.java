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

package io.openraven.magpie.core.dmap.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.core.dmap.client.dto.AppProbability;
import io.openraven.magpie.core.dmap.client.dto.DMapMLRequest;
import io.openraven.magpie.core.dmap.client.dto.DMapMLResponse;
import io.openraven.magpie.core.dmap.exception.DMapClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class DMapMLClientImpl implements DMapMLClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(DMapMLClientImpl.class);
  private static final String DMAP_ML_PROP_NAME = "openraven.dmap.ml";

  private final String dmapMLServiceURL;

  private final ObjectMapper mapper;

  public DMapMLClientImpl(ObjectMapper mapper, MagpieConfig config) {
    this.mapper = mapper;
    this.dmapMLServiceURL = getDmapMlUrl(config);
  }

  @Override
  public List<AppProbability> predict(Map<String, String> signature) {
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(dmapMLServiceURL))
      .POST(HttpRequest.BodyPublishers.ofString(getRequestBody(signature)))
      .header("Content-Type", "application/json")
      .build();

    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      DMapMLResponse dMapMLResponse = mapper.readValue(response.body(), DMapMLResponse.class);
      return dMapMLResponse.getPredictions();
    } catch (Exception e) {
      LOGGER.error("Unable to send request to OpenRaven DMAP ML service", e);
      throw new RuntimeException(e);
    }
  }

  private String getRequestBody(Map<String, String> signature) {
    DMapMLRequest dMapMLRequest = new DMapMLRequest();
    dMapMLRequest.setSignature(signature);
    try {
      return mapper.writeValueAsString(dMapMLRequest);
    } catch (JsonProcessingException e) {
      LOGGER.error("Unable to serialize DMap ML request body: {}", dMapMLRequest, e);
      throw new DMapClientException("DMap response deserialization failed", e);
    }
  }

  private String getDmapMlUrl(MagpieConfig config) {
    return config.getServices().get(DMAP_ML_PROP_NAME).getUrl();
  }
}
