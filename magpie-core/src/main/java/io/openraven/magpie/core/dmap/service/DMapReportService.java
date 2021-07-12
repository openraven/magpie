package io.openraven.magpie.core.dmap.service;

import io.openraven.magpie.core.dmap.dto.DMapScanResult;

public interface DMapReportService {

  void generateReport(DMapScanResult dMapScanResult);

}
