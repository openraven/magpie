package io.openraven.magpie.plugins.gdrive.discovery.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.CredentialsProvider;
import com.google.appengine.repackaged.com.google.common.base.Pair;

import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.MagpieGdriveResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gcp.storage.StorageBucket;
import io.openraven.magpie.data.gdrive.drive.Drive;
import io.openraven.magpie.plugins.gdrive.discovery.GDriveUtils;
import io.openraven.magpie.plugins.gdrive.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

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
  public void discover(mapper, session, emitter, logger) {
    final String RESOURCE_TYPE = Drive.RESOURCE_TYPE;

    final StorageOptions.Builder builder = StorageOptions.newBuilder();
    try {
      if(maybeCredentialsProvider.isPresent()){
        builder.setCredentials(maybeCredentialsProvider.get().getCredentials());
      }
    }catch(IOException ioException) {
      throw new RuntimeException(ioException);
    }
    Drive drive = builder.setDomain(domain).build().getService();
    drive.list().iterateAll().forEach(drive -> {
      var data = new MagpieGdriveResource.MagpieGdriveResourceBuilder(mapper)
        .withDomain(domain)
        .withResourceType(RESOURCE_TYPE)
        .withConfiguration(GDriveUtils.asJsonNode(drive))
        .build();


      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":drive"), data.toJsonNode()));
    });
  }

}
