package io.openraven.magpie.plugins.gdrive.discovery.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.CredentialsProvider;

import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.MagpieGdriveResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gdrive.drive.Drive;
import io.openraven.magpie.plugins.gdrive.discovery.GDriveUtils;
import io.openraven.magpie.plugins.gdrive.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import io.openraven.magpie.plugins.gdrive.discovery.services.GDriveDiscovery;


public class DriveDiscovery implements GDriveDiscovery {

  private static final String SERVICE = "drive";


  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, Session session, Emitter emitter, Logger logger) {
    final String RESOURCE_TYPE = Drive.RESOURCE_TYPE;



  }
}


