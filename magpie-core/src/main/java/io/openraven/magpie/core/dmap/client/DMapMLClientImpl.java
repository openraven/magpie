package io.openraven.magpie.core.dmap.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.core.dmap.client.dto.AppProbability;
import io.openraven.magpie.core.dmap.client.dto.DMapMLRequest;
import io.openraven.magpie.core.dmap.client.dto.DMapMLResponse;
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
  private static final String DMAP_ML_SERVICE_URL = "https://api.openraven.com/dmap-ml/predict-annotated";

  private final ObjectMapper objectMapper;

  public DMapMLClientImpl(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public List<AppProbability> predict(Map<String, String> signature) {
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(DMAP_ML_SERVICE_URL))
      .POST(HttpRequest.BodyPublishers.ofString(getRequestBody(signature)))
      .header("Content-Type", "application/json")
      .build();

    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      DMapMLResponse dMapMLResponse = objectMapper.readValue(response.body(), DMapMLResponse.class);
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
      return objectMapper.writeValueAsString(dMapMLRequest);
    } catch (JsonProcessingException e) {
      LOGGER.error("Unable to serialize DMap ML request body: {}", dMapMLRequest, e);
      throw new RuntimeException(e);
    }
  }
}
