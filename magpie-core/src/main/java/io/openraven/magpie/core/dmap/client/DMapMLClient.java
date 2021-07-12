package io.openraven.magpie.core.dmap.client;

import io.openraven.magpie.core.dmap.client.dto.AppProbability;

import java.util.List;
import java.util.Map;

public interface DMapMLClient {

  List<AppProbability> predict(Map<String, String> request);

}
