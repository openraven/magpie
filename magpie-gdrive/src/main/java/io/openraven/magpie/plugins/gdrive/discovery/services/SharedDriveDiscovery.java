package io.openraven.magpie.plugins.gdrive.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.CredentialsProvider;

import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.MagpieGdriveResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gdrive.drive.Drive;
import io.openraven.magpie.data.gdrive.shareddrive.SharedDrive;
import io.openraven.magpie.plugins.gdrive.discovery.GDriveUtils;
import io.openraven.magpie.plugins.gdrive.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class SharedDriveDiscovery implements GDriveDiscovery{

  private static final String SERVICE = "shareddrive";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public void discoverSharedDrives(mapper, session, emitter, logger) {
    final String RESOURCE_TYPE = SharedDrive.RESOURCE_TYPE;

  }

}
