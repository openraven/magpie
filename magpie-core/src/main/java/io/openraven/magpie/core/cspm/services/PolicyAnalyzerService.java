package io.openraven.magpie.core.cspm.services;

import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.core.cspm.model.PolicyContext;
import io.openraven.magpie.core.cspm.model.Rule;
import io.openraven.magpie.core.cspm.analysis.ScanResults;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface PolicyAnalyzerService {

  void init(MagpieConfig config);


  /**
   * Using the supplied List of Policies, execute each Policy's Rules (SQL query) against the database credentials provided in
   * MagpieConfig.  Each query is expected to return at a minimum a field named 'arn' for each row matched.  If no rows
   * are matched then no violation occured for that Rule.
   *
   * @param policies The List of PolicyContexts to match against.
   * @return A list of violations, or an empty list of none are found.
   * @throws IOException
   */
  ScanResults analyze(List<PolicyContext> policies) throws Exception;

  List<Map<String, Object>> evaluate(Rule rule, Object resultSet) throws Exception;
}
