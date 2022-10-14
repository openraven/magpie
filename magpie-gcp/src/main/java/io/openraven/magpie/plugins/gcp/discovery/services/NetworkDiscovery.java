/*
 * Copyright 2021 Open Raven Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openraven.magpie.plugins.gcp.discovery.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.CredentialsProvider;
import com.google.appengine.repackaged.com.google.common.base.Pair;
import com.google.cloud.compute.v1.Network;
import com.google.cloud.compute.v1.NetworkClient;
import com.google.cloud.compute.v1.NetworkSettings;
import com.google.cloud.compute.v1.Subnetwork;
import com.google.cloud.compute.v1.SubnetworkClient;
import com.google.cloud.compute.v1.SubnetworkSettings;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NetworkDiscovery implements GCPDiscovery {
  private static final String SERVICE = "vpc";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger, Optional<CredentialsProvider> maybeCredentialsProvider) {
    final String RESOURCE_TYPE = io.openraven.magpie.data.gcp.vpc.Network.RESOURCE_TYPE;
    var networkSettingsBuilder = NetworkSettings.newBuilder();
    var subnetworkSettingsBuilder = SubnetworkSettings.newBuilder();
    maybeCredentialsProvider.ifPresent(provider -> {
        networkSettingsBuilder.setCredentialsProvider(provider);
        subnetworkSettingsBuilder.setCredentialsProvider(provider);
    });
    try (NetworkClient networkClient = NetworkClient.create(networkSettingsBuilder.build());
         SubnetworkClient subnetworkClient = SubnetworkClient.create(subnetworkSettingsBuilder.build())) {
      networkClient.listNetworks(projectId).iterateAll().forEach(network -> {
        var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, network.getName())
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

  private void discoverSubnetworks(SubnetworkClient subnetworkClient, Network network, MagpieGcpResource data) {
    final String fieldName = "subnetworks";

    List<Subnetwork.Builder> subnetworks = new ArrayList<>();
    network.getSubnetworksList().forEach(subnetStr -> {
      Subnetwork subnetwork = subnetworkClient.getSubnetwork(subnetStr);
      subnetworks.add(subnetwork.toBuilder());
    });

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, subnetworks));

  }

}
