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
import com.google.appengine.repackaged.com.google.common.base.Pair;
import com.google.cloud.bigquery.reservation.v1.Assignment;
import com.google.cloud.bigquery.reservation.v1.LocationName;
import com.google.cloud.bigquery.reservation.v1.Reservation;
import com.google.cloud.bigquery.reservation.v1.ReservationServiceClient;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BigQueryReservationDiscovery implements GCPDiscovery {
  private static final String SERVICE = "bigQueryReservation";

  // https://cloud.google.com/bigquery/docs/locations
  private static final List<String> AVAILABLE_LOCATIONS = List.of(
    "asia-east1",
    "asia-northeast1",
    "asia-northeast2",
    "asia-northeast3",
    "asia-south1",
    "asia-south2",
    "asia-southeast1",
    "asia-southeast2",
    "australia-southeast1",
    "eu",
    "europe-central2",
    "europe-north1",
    "europe-west2",
    "europe-west3",
    "europe-west4",
    "europe-west6",
    "northamerica-northeast1",
    "southamerica-east1",
    "us",
    "us-central1",
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

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger) {
    try (var client = ReservationServiceClient.create()) {
      discoverReservations(mapper, projectId, session, emitter, client);
      discoverCapacityCommitments(mapper, projectId, session, emitter, client);
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException("BigQueryReservation", e);
    }
  }

  private void discoverReservations(ObjectMapper mapper, String projectId, Session session, Emitter emitter, ReservationServiceClient client) {
    final String RESOURCE_TYPE = "GCP::BigQueryReservation::Reservation";

    AVAILABLE_LOCATIONS.forEach(location -> {
      String parent = LocationName.of(projectId, location).toString();

      for (var reservation : client.listReservations(parent).iterateAll()) {
        var data = new MagpieResource.MagpieResourceBuilder(mapper, reservation.getName())
          .withProjectId(projectId)
          .withResourceType(RESOURCE_TYPE)
          .withRegion(location)
          .withConfiguration(GCPUtils.asJsonNode(reservation))
          .build();

        discoverAssignments(client, reservation, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":reservation"), data.toJsonNode()));
      }
    });
  }

  private void discoverAssignments(ReservationServiceClient client, Reservation reservation, MagpieResource data) {
    final String fieldName = "assignments";

    ArrayList<Assignment.Builder> list = new ArrayList<>();
    client.listAssignments(reservation.getName()).iterateAll()
      .forEach(assignment -> list.add(assignment.toBuilder()));

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, list));
  }

  private void discoverCapacityCommitments(ObjectMapper mapper, String projectId, Session session, Emitter emitter, ReservationServiceClient client) {
    final String RESOURCE_TYPE = "GCP::BigQueryReservation::CapacityCommitment";

    AVAILABLE_LOCATIONS.forEach(location -> {
      String parent = LocationName.of(projectId, location).toString();

      for (var capacityCommitment : client.listCapacityCommitments(parent).iterateAll()) {
        var data = new MagpieResource.MagpieResourceBuilder(mapper, capacityCommitment.getName())
          .withProjectId(projectId)
          .withResourceType(RESOURCE_TYPE)
          .withRegion(location)
          .withConfiguration(GCPUtils.asJsonNode(capacityCommitment))
          .build();

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":capacityCommitment"), data.toJsonNode()));
      }
    });
  }
}
