package io.openraven.nightglow.plugins.aws.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.nightglow.api.Emitter;
import io.openraven.nightglow.api.Session;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;

public interface AWSDiscovery{
  void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger);
}
