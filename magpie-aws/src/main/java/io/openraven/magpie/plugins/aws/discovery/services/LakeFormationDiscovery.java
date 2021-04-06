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
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lakeformation.LakeFormationClient;
import software.amazon.awssdk.services.lakeformation.model.GetDataLakeSettingsRequest;
import software.amazon.awssdk.services.lakeformation.model.GetEffectivePermissionsForPathRequest;
import software.amazon.awssdk.services.lakeformation.model.ListResourcesRequest;
import software.amazon.awssdk.services.lakeformation.model.ResourceInfo;

import java.util.List;
import java.util.Map;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class LakeFormationDiscovery implements AWSDiscovery {

  private static final String SERVICE = "lakeFormation";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return LakeFormationClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger) {
    final var client = LakeFormationClient.builder().region(region).build();

    getAwsResponse(
      () -> client.listResourcesPaginator(ListResourcesRequest.builder().build()).stream(),
      (resp) -> resp.forEach(list -> list.resourceInfoList().forEach(resourceInfo -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", resourceInfo.toBuilder());
        data.put("region", region.toString());

        discoverDataLakeSettings(client, data);
        discoverPermissions(client, resourceInfo, data);

        emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":resource"), data));
      })),
      (noresp) -> logger.error("Failed to get resources in {}", region)
    );
  }

  private void discoverDataLakeSettings(LakeFormationClient client, ObjectNode data) {
    final String keyname = "dataLakeSettings";

    getAwsResponse(
      () -> client.getDataLakeSettings(GetDataLakeSettingsRequest.builder().build()).dataLakeSettings(),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private void discoverPermissions(LakeFormationClient client, ResourceInfo resource, ObjectNode data) {
    final String keyname = "permissions";

    getAwsResponse(
      () -> client.getEffectivePermissionsForPath(GetEffectivePermissionsForPathRequest.builder().resourceArn(resource.resourceArn()).build()).permissions(),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }
}
