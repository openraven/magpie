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
import com.google.cloud.servicedirectory.v1.Endpoint;
import com.google.cloud.servicedirectory.v1.LocationName;
import com.google.cloud.servicedirectory.v1.Namespace;
import com.google.cloud.servicedirectory.v1.RegistrationServiceClient;
import com.google.cloud.servicedirectory.v1.RegistrationServiceSettings;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gcp.service.Service;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServiceDirectoryDiscovery implements GCPDiscovery {
  private static final String SERVICE = "serviceDirectory";

  private static final List<String> AVAILABLE_LOCATIONS = List.of(
    "asia-east1",
    "asia-east2",
    "asia-northeast1",
    "asia-northeast2",
    "asia-northeast3",
    "asia-south1",
    "asia-south2",
    "asia-southeast1",
    "asia-southeast2",
    "australia-southeast1",
    "australia-southeast2",
    "europe-central2",
    "europe-north1",
    "europe-west1",
    "europe-west2",
    "europe-west3",
    "europe-west4",
    "europe-west5",
    "europe-west6",
    "northamerica-northeast1",
    "northamerica-northeast2",
    "southamerica-east1",
    "us-central1",
    "us-central2",
    "us-east1",
    "us-east4",
    "us-west1",
    "us-west2",
    "us-west3",
    "us-west4");

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger, Optional<CredentialsProvider> maybeCredentialsProvider) {
    final String RESOURCE_TYPE = Service.RESOURCE_TYPE;
    var builder = RegistrationServiceSettings.newBuilder();
    maybeCredentialsProvider.ifPresent(builder::setCredentialsProvider);

    try (RegistrationServiceClient registrationServiceClient = RegistrationServiceClient.create(builder.build())) {
      AVAILABLE_LOCATIONS.forEach(location -> {  // Discover services in all namespaces for all locations
        String parent = LocationName.of(projectId, location).toString();

        registrationServiceClient.listNamespaces(parent).iterateAll().forEach(namespace -> {

          registrationServiceClient.listServices(namespace.getName()).iterateAll().forEach(service -> {
            var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, service.getName())
              .withProjectId(projectId)
              .withResourceType(RESOURCE_TYPE)
              .withRegion(location)
              .withConfiguration(GCPUtils.asJsonNode(service.toBuilder()))
              .build();

            addNamespaceConfiguration(namespace, data);
            discoverEndpoints(registrationServiceClient, service.getName(), data);

            emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":service"), data.toJsonNode()));
          });

        });
      });
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void addNamespaceConfiguration(Namespace namespace, MagpieGcpResource data) {
    final String fieldName = "namespace";
    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, namespace.toBuilder()));

  }

  private void discoverEndpoints(RegistrationServiceClient registrationServiceClient,
                                 String serviceName,
                                 MagpieGcpResource data) {
    final String fieldName = "endpoints";

    List<Endpoint.Builder> endpoints = new ArrayList<>();
    registrationServiceClient.listEndpoints(serviceName).iterateAll()
      .forEach(endpoint -> endpoints.add(endpoint.toBuilder()));

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, endpoints));
  }

}
