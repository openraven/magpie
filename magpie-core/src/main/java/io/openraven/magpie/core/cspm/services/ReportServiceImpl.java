package io.openraven.magpie.core.cspm.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.openraven.magpie.core.cspm.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class ReportServiceImpl implements ReportService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReportServiceImpl.class);
  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE));

  public ReportServiceImpl() {
    MAPPER.findAndRegisterModules();
    MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  }

  @Override
  public void generateReport(List<PolicyContext> policies, List<Violation> violations) {
    if (violations.isEmpty()) {
      System.out.println("Congratulations, you have no violated rules!");
    } else {
      System.out.println("There are following violations:");
      try {
        MAPPER.writeValue(System.out, violations);
      } catch (IOException e) {
        LOGGER.error("Generating report failed: {}", e.getMessage());
      }
    }
  }
}
