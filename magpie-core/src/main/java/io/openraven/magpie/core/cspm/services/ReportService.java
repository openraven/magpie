package io.openraven.magpie.core.cspm.services;

import io.openraven.magpie.core.cspm.Violation;

import java.util.List;

public interface ReportService {
  void generateReport(List<PolicyContext> policies, List<Violation> violations);
}
