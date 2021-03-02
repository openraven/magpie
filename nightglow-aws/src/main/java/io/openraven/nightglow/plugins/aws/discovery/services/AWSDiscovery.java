package io.openraven.nightglow.plugins.aws.discovery.services;

import io.openraven.nightglow.api.Emitter;
import io.openraven.nightglow.api.Session;

public interface AWSDiscovery {

  void discover(Session session, Emitter emitter);
}
