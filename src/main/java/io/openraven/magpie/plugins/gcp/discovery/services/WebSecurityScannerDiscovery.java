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
import com.google.cloud.websecurityscanner.v1.*;
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

public class WebSecurityScannerDiscovery implements GCPDiscovery {
  private static final String SERVICE = "webSecurityScanner";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(String projectId, Session session, Emitter emitter, Logger logger) {
    final String RESOURCE_TYPE = "GCP::WebSecurityScanner::ScanConfig";

    try (WebSecurityScannerClient webSecurityScannerClient = WebSecurityScannerClient.create()) {
      ListScanConfigsRequest request =
        ListScanConfigsRequest.newBuilder()
          .setParent(String.format("projects/%s", projectId))
          .build();
      for (ScanConfig element : webSecurityScannerClient.listScanConfigs(request).iterateAll()) {
        var data = new GCPResource(element.getName(), projectId, RESOURCE_TYPE);
        data.configuration = GCPUtils.asJsonNode(element);

        discoverScanRuns(webSecurityScannerClient, element, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":scanConfig"), data.toJsonNode()));
      }
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }

  private void discoverScanRuns(WebSecurityScannerClient client,
                                ScanConfig scanConfig,
                                GCPResource data) {
    final String fieldName = "scanRuns";

    ArrayList<ScanRun> list = new ArrayList<>();

    ListScanRunsRequest request =
      ListScanRunsRequest.newBuilder()
        .setParent(scanConfig.getName())
        .build();
    for (ScanRun element : client.listScanRuns(request).iterateAll()) {
      list.add(element);
    }

    GCPUtils.update(data.supplementaryConfiguration, Pair.of(fieldName, list));
  }
}
