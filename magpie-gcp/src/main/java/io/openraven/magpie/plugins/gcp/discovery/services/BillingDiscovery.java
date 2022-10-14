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
import com.google.cloud.billing.v1.BillingAccount;
import com.google.cloud.billing.v1.CloudBillingClient;
import com.google.cloud.billing.v1.CloudBillingSettings;
import com.google.cloud.billing.v1.ProjectBillingInfo;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BillingDiscovery implements GCPDiscovery {
  private static final String SERVICE = "billing";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger, Optional<CredentialsProvider> maybeCredentialsProvider) {
    final String RESOURCE_TYPE = io.openraven.magpie.data.gcp.billing.BillingAccount.RESOURCE_TYPE;
    var builder = CloudBillingSettings.newBuilder();
    maybeCredentialsProvider.ifPresent(builder::setCredentialsProvider);

    try (var client = CloudBillingClient.create(builder.build())) {
      for (var billingAccount : client.listBillingAccounts().iterateAll()) {
        var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, billingAccount.getName())
          .withProjectId(projectId)
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(GCPUtils.asJsonNode(billingAccount))
          .build();

        discoverProjectBillingInfo(client, billingAccount, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":billingAccount"), data.toJsonNode()));
      }
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverProjectBillingInfo(CloudBillingClient client, BillingAccount billingAccount, MagpieGcpResource data) {
    final String fieldName = "projectBillingInfo";

    ArrayList<ProjectBillingInfo.Builder> list = new ArrayList<>();
    client.listProjectBillingInfo(billingAccount.getName()).iterateAll()
      .forEach(task -> list.add(task.toBuilder()));

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, list));
  }
}
