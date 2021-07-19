package io.openraven.magpie.plugins.aws.discovery;

import io.openraven.magpie.plugins.aws.discovery.services.AWSDiscovery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.amazon.awssdk.regions.Region.*;

@ExtendWith(MockitoExtension.class)
public class AWSDiscoveryPluginTest {

  private static final List<Region> PLUGIN_SUPPORTED_REGIONS = List.of(
    US_EAST_1,
    US_EAST_2,
    US_WEST_1,
    EU_CENTRAL_1,
    AP_SOUTH_1);

  private static final List<String> CONFIG_ENABLED_REGIONS = List.of(
    US_EAST_1.id(),    // filtered by .*east.*
    US_EAST_2.id(),    // filtered by .*east.*
    US_WEST_1.id(),    // filtered by us-west-1
    EU_CENTRAL_1.id(), // filtered by eu.*
    EU_NORTH_1.id(),   // filtered by eu.*
    AWS_GLOBAL.id(),   // not a plugin supported
    AP_SOUTH_1.id());  // <- Matched. Allowed

  private static final List<String> CONFIG_IGNORED_REGION_PATTERNS = List.of("eu.*", ".*east.*", "us-west-1");

  private AWSDiscoveryConfig awsDiscoveryConfig = new AWSDiscoveryConfig();
  private AWSDiscoveryPlugin awsDiscoveryPlugin = new AWSDiscoveryPlugin();

  @Mock
  private AWSDiscovery awsDiscoveryMock;
  @Mock
  private Logger logger;


  @Test
  public void testFilteringForIgnoredRegions() {
    awsDiscoveryConfig.setRegions(CONFIG_ENABLED_REGIONS);
    awsDiscoveryConfig.setIgnoreRegions(CONFIG_IGNORED_REGION_PATTERNS);
    Mockito.when(awsDiscoveryMock.getSupportedRegions()).thenReturn(PLUGIN_SUPPORTED_REGIONS);

    // execute
    awsDiscoveryPlugin.init(awsDiscoveryConfig, logger);
    List<Region> regions = awsDiscoveryPlugin.getRegionsForDiscovery(awsDiscoveryMock);

    assertEquals(1, regions.size());
    assertEquals(AP_SOUTH_1, regions.get(0));

  }

  // Forced to copy this method due to unavailability of testing
  private boolean isNotIgnoredRegion(String region) {
    return awsDiscoveryConfig.getIgnoreRegions()
      .stream()
      .noneMatch(pattern -> Pattern.matches(pattern, region));
  }
}
