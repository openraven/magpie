package io.openraven.magpie.core.cspm.services.report;

import io.openraven.magpie.core.cspm.ScanMetadata;
import io.openraven.magpie.core.cspm.services.ReportService;

import java.util.List;

public enum ReportFormat {

  VERBOSE("verbose") {
    @Override
    public ReportService build(ScanMetadata scanMetadata) {
      return new VerboseReportServiceImpl(scanMetadata);
    }
  },

  CSV("csv") {
    @Override
    public ReportService build(ScanMetadata scanMetadata) {
      return new CsvReportService(scanMetadata);
    }
  },

  JSON("json") {
    @Override
    public ReportService build(ScanMetadata scanMetadata) {
      return new JsonReportService(scanMetadata);
    }
  };

  private String type;

  ReportFormat(String type) {
    this.type = type;
  }

  public abstract ReportService build(ScanMetadata scanMetadata);

  public static ReportFormat getByType(String type) {
    for (ReportFormat reportFormat : values()) {
      if (reportFormat.type.equals(type)) {
        return reportFormat;
      }
    }
    throw new IllegalArgumentException("Unknown report type: " + type + ". Valid options: " + List.of(values()));
  }

}
