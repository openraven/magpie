package io.openraven.magpie.plugins.gdrive.discovery.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.CredentialsProvider;
import com.google.appengine.repackaged.com.google.common.base.Pair;

import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGdriveResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gdrive.drive.Drive;
import io.openraven.magpie.plugins.gdrive.discovery.GDriveUtils;
import io.openraven.magpie.plugins.gdrive.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.batch.BatchClient;

import java.io.IOException;
import java.util.List;
import java.util.Optional;


public class DriveDiscovery implements GDriveDiscovery{

  private static final String SERVICE = "drive";


  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public void discover(mapper, driveId, session, emitter, logger) {
    try (final var client = clientCreator.apply(BatchClient.builder()).build()) {
      discoverComputeEnvironments(mapper, session, client, region, emitter, account);
      discoverJobQueues(mapper, session, client, region, emitter, account);
      discoverJobDefinitions(mapper, session, client, region, emitter, account);
    }
  }

}
