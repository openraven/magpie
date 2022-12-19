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
import com.google.cloud.recaptchaenterprise.v1.RecaptchaEnterpriseServiceClient;
import com.google.cloud.recaptchaenterprise.v1.RecaptchaEnterpriseServiceSettings;
import com.google.recaptchaenterprise.v1.Key;
import com.google.recaptchaenterprise.v1.ListKeysRequest;
import com.google.recaptchaenterprise.v1.ProjectName;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieGcpResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.gcp.recaptcha.RecaptchaEnterpriseKey;
import io.openraven.magpie.plugins.gcp.discovery.exception.DiscoveryExceptions;
import io.openraven.magpie.plugins.gcp.discovery.GCPUtils;
import io.openraven.magpie.plugins.gcp.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class RecaptchaEnterpriseDiscovery implements GCPDiscovery {
  private static final String SERVICE = "recaptchaEnterprise";

  @Override
  public String service() {
    return SERVICE;
  }

  public void discover(ObjectMapper mapper, String projectId, Session session, Emitter emitter, Logger logger, Optional<CredentialsProvider> maybeCredentialsProvider) {
    final String RESOURCE_TYPE = RecaptchaEnterpriseKey.RESOURCE_TYPE;
    var builder = RecaptchaEnterpriseServiceSettings.newBuilder();
    maybeCredentialsProvider.ifPresent(builder::setCredentialsProvider);

    try (RecaptchaEnterpriseServiceClient recaptchaEnterpriseServiceClient =
           RecaptchaEnterpriseServiceClient.create(builder.build())) {
      ListKeysRequest request =
        ListKeysRequest.newBuilder()
          .setParent(ProjectName.of(projectId).toString())
          .build();
      for (Key key : recaptchaEnterpriseServiceClient.listKeys(request).iterateAll()) {
        var data = new MagpieGcpResource.MagpieGcpResourceBuilder(mapper, key.getName())
          .withProjectId(projectId)
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(GCPUtils.asJsonNode(key))
          .build();

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":key"), data.toJsonNode()));
      }
    } catch (IOException e) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, e);
    }
  }
}
