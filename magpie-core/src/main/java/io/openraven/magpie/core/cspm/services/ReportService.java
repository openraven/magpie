package io.openraven.magpie.core.cspm.services;

import io.openraven.magpie.core.cspm.ScanResults;

public interface ReportService {
  void generateReport(ScanResults results);
}
