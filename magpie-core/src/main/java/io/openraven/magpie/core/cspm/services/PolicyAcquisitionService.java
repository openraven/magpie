package io.openraven.magpie.core.cspm.services;

import io.openraven.magpie.api.cspm.PolicyContext;
import io.openraven.magpie.core.config.MagpieConfig;

import java.io.IOException;
import java.util.List;

public interface PolicyAcquisitionService {
  void init(MagpieConfig config);
  List<PolicyContext> loadPolicies() throws IOException;
}
