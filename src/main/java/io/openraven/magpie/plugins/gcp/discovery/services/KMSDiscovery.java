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

import com.google.appengine.repackaged.com.google.common.base.Pair;
import com.google.cloud.kms.v1.CryptoKey;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.cloud.kms.v1.LocationName;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPResource;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class KMSDiscovery implements GCPDiscovery {
  private static final String SERVICE = "kms";

  private static final List<String> AVAILABLE_LOCATIONS = List.of(
    "asia",
    "asia1",
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
    "eur3",
    "eur4",
    "eur5",
    "europe",
    "europe-central2",
    "europe-north1",
    "europe-west1",
    "europe-west2",
    "europe-west3",
    "europe-west4",
    "europe-west5",
    "europe-west6",
    "global",
    "nam3",
    "nam4",
    "nam6",
    "nam9",
    "northamerica-northeast1",
    "southamerica-east1",
    "us",
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

  public void discover(String projectId, Session session, Emitter emitter, Logger logger) {
    final String RESOURCE_TYPE = "GCP::KMS::Keyring";
    try (KeyManagementServiceClient keyManagementServiceClient = KeyManagementServiceClient.create()) {
      AVAILABLE_LOCATIONS.forEach(location -> {
        String parent = LocationName.of(projectId, location).toString();
        keyManagementServiceClient.listKeyRings(parent).iterateAll().forEach(keyRing -> {
          var data = new GCPResource(keyRing.getName(), projectId, RESOURCE_TYPE);
          data.configuration = GCPUtils.asJsonNode(keyRing);

          discoverKeys(keyManagementServiceClient, keyRing, data);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":keyring"), data.toJsonNode()));
        });
      });
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverKeys(KeyManagementServiceClient keyManagementServiceClient, com.google.cloud.kms.v1.KeyRing keyRing, GCPResource data) {
    final String fieldName = "keys";

    ArrayList<CryptoKey.Builder> list = new ArrayList<>();
    keyManagementServiceClient.listCryptoKeys(keyRing.getName()).iterateAll()
      .forEach(device -> list.add(device.toBuilder()));

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, list));
  }
}
