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

package io.openraven.magpie.plugins.aws.discovery.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.guardduty.GuardDutyClient;
import software.amazon.awssdk.services.guardduty.model.GetDetectorRequest;

import java.time.Instant;
import java.util.List;

public class GuardDutyDiscovery implements AWSDiscovery {

  private static final String SERVICE = "guardduty";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return GuardDutyClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    final var client = AWSUtils.configure(GuardDutyClient.builder(), region);

    final String RESOURCE_TYPE = "AWS::GuardDuty::Detector";

    try {
      client.listDetectorsPaginator()
        .forEach(detector -> detector.detectorIds().forEach(
          id -> {
            final var resp = client.getDetector(GetDetectorRequest.builder().detectorId(id).build());

            var data = new MagpieResource.MagpieResourceBuilder(mapper, "arn:aws:guardduty:::detector/" + id)
              .withResourceName(id)
              .withResourceId(id)
              .withResourceType(RESOURCE_TYPE)
              .withConfiguration(mapper.valueToTree(resp.toBuilder()))
              .withCreatedIso(Instant.parse(resp.createdAt()))
              .withAccountId(account)
              .withRegion(region.toString())
              .build();

            AWSUtils.update(data.tags, mapper.convertValue(resp.tags(), JsonNode.class));
            emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":backupVault"), data.toJsonNode()));
          })
        );
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }
}
