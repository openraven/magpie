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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSResource;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.config.ConfigClient;
import software.amazon.awssdk.services.config.model.ConfigurationRecorder;
import software.amazon.awssdk.services.config.model.DescribeConfigurationRecorderStatusRequest;

import java.util.List;
import java.util.Map;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class ConfigDiscovery implements AWSDiscovery {

  private static final String SERVICE = "config";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return ConfigClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    final var client = ConfigClient.builder().region(region).build();
    final String RESOURCE_TYPE = "AWS::Config::Recorder";

    try {
      client.describeConfigurationRecorders().configurationRecorders()
        .forEach(configurationRecorder -> {
          var data = new AWSResource(configurationRecorder.toBuilder(), region.toString(), account, mapper);
          data.arn = configurationRecorder.roleARN();
          data.resourceName = configurationRecorder.name();
          data.resourceType = RESOURCE_TYPE;

          discoverConfigurationRecorderStatus(client, configurationRecorder, data);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":configurationRecorder"), data.toJsonNode(mapper)));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverConfigurationRecorderStatus(ConfigClient client, ConfigurationRecorder resource, AWSResource data) {
    final String keyname = "status";

    var request =
      DescribeConfigurationRecorderStatusRequest.builder().configurationRecorderNames(resource.name()).build();

    getAwsResponse(
      () -> client.describeConfigurationRecorderStatus(request).configurationRecordersStatus(),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp.get(0))),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }
}
