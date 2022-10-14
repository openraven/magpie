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
import com.google.cloud.dns.*;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gcp.dns.DnsZone;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DnsDiscovery implements GCPDiscovery {
  private static final String SERVICE = "dns";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger, Optional<CredentialsProvider> maybeCredentialsProvider) {
    final String RESOURCE_TYPE = DnsZone.RESOURCE_TYPE;
    var builder = DnsOptions.newBuilder();
    maybeCredentialsProvider.ifPresent(provider -> {
        try {
            builder.setCredentials(provider.getCredentials());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    });
    var dnsInstance = builder.build().getService();

    dnsInstance.listZones().iterateAll().forEach(zone -> {
        var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, zone.getName())
          .withProjectId(projectId)
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(GCPUtils.asJsonNode(zone))
          .build();

        discoverChangeRequests(dnsInstance, zone, data);
        discoverRecordSets(dnsInstance, zone, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":zone"), data.toJsonNode()));
      });
  }

  private void discoverChangeRequests(Dns dnsInstance, Zone zone, MagpieGcpResource data) {
    final String fieldName = "changeRequests";

    ArrayList<ChangeRequest.Builder> list = new ArrayList<>();
    dnsInstance.listChangeRequests(zone.getName()).iterateAll()
      .forEach(changeRequest -> list.add(changeRequest.toBuilder()));

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, list));
  }

  private void discoverRecordSets(Dns dnsInstance, Zone zone, MagpieGcpResource data) {
    final String fieldName = "recordSets";

    ArrayList<RecordSet.Builder> list = new ArrayList<>();
    dnsInstance.listRecordSets(zone.getName()).iterateAll()
      .forEach(recordSet -> list.add(recordSet.toBuilder()));

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, list));
  }
}
