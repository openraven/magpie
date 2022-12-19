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
import com.google.cloud.vision.v1.LocationName;
import com.google.cloud.vision.v1.ProductSearchClient;
import com.google.cloud.vision.v1.ProductSearchSettings;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gcp.vision.Product;
import io.openraven.magpie.data.gcp.vision.ProductSet;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class VisionDiscovery implements GCPDiscovery {
  private static final String SERVICE = "vision";

  private static final List<String> AVAILABLE_LOCATIONS = List.of(
    "us-west1",
    "us-east1",
    "asia-east1",
    "europe-west1");

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger, Optional<CredentialsProvider> maybeCredentialsProvider) {
    var builder = ProductSearchSettings.newBuilder();
    maybeCredentialsProvider.ifPresent(builder::setCredentialsProvider);
    try (ProductSearchClient productSearchClient = ProductSearchClient.create(builder.build())) {
      AVAILABLE_LOCATIONS.forEach(location -> {
        discoverProducts(mapper, projectId, session, emitter, productSearchClient, location);
        discoverProductSets(mapper, projectId, session, emitter, productSearchClient, location);
      });

    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException("GCP::Vision", e);
    }
  }

  private void discoverProducts(ObjectMapper mapper, String projectId, Session session, Emitter emitter, ProductSearchClient productSearchClient, String location) {
    final String RESOURCE_TYPE = Product.RESOURCE_TYPE;

    LocationName parent = LocationName.of(projectId, location);
    for (var product : productSearchClient.listProducts(parent).iterateAll()) {
      var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, product.getName())
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withRegion(location)
        .withConfiguration(GCPUtils.asJsonNode(product))
        .build();

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":product"), data.toJsonNode()));
    }
  }

  private void discoverProductSets(ObjectMapper mapper, String projectId, Session session, Emitter emitter, ProductSearchClient productSearchClient, String location) {
    final String RESOURCE_TYPE = ProductSet.RESOURCE_TYPE;

    LocationName parent = LocationName.of(projectId, location);
    for (var product : productSearchClient.listProductSets(parent).iterateAll()) {
      var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, product.getName())
        .withProjectId(projectId)
        .withResourceType(RESOURCE_TYPE)
        .withRegion(location)
        .withConfiguration(GCPUtils.asJsonNode(product))
        .build();

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":productSet"), data.toJsonNode()));
    }
  }
}
