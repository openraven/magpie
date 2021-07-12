package io.openraven.magpie.core.dmap.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DMapMLResponse {

  private List<AppProbability> predictions;

  public List<AppProbability> getPredictions() {
    return predictions;
  }

  public void setPredictions(List<AppProbability> predictions) {
    this.predictions = predictions;
  }
}
