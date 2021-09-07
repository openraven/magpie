package io.openraven.magpie.plugins.gcp.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.repackaged.com.google.common.base.Pair;
import com.google.cloud.compute.v1.Network;
import com.google.cloud.compute.v1.NetworkClient;
import com.google.cloud.compute.v1.Subnetwork;
import com.google.cloud.compute.v1.SubnetworkClient;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NetworkDiscovery implements GCPDiscovery {
  private static final String SERVICE = "vpc";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger) {
    final String RESOURCE_TYPE = "GCP::VPC::Network";

    try (NetworkClient networkClient = NetworkClient.create();
         SubnetworkClient subnetworkClient = SubnetworkClient.create()) {
      networkClient.listNetworks(projectId).iterateAll().forEach(network -> {
        var data = new MagpieResource.MagpieResourceBuilder(mapper, network.getName())
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withConfiguration(GCPUtils.asJsonNode(network))
        .build();

        discoverSubnetworks(subnetworkClient, network, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":network"), data.toJsonNode()));
      });

    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverSubnetworks(SubnetworkClient subnetworkClient, Network network, MagpieResource data) {
    final String fieldName = "subnetworks";

    List<Subnetwork.Builder> subnetworks = new ArrayList<>();
    network.getSubnetworksList().forEach(subnetStr -> {
      Subnetwork subnetwork = subnetworkClient.getSubnetwork(subnetStr);
      subnetworks.add(subnetwork.toBuilder());
    });

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, subnetworks));

  }

}
