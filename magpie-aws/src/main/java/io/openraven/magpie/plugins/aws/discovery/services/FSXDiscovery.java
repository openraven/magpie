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
import io.openraven.magpie.plugins.aws.discovery.Conversions;
import org.javatuples.Pair;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.fsx.FSxClient;
import software.amazon.awssdk.services.fsx.model.FileSystem;

import java.util.ArrayList;
import java.util.List;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;
import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getCloudwatchMetricMinimum;

public class FSXDiscovery implements AWSDiscovery {

  private static final String SERVICE = "fsx";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return FSxClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger) {
    final var client = FSxClient.builder().region(region).build();

    getAwsResponse(
      () -> client.describeFileSystems().fileSystems().stream(),
      (resp) -> resp.forEach(fileSystem -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", fileSystem.toBuilder());
        data.put("region", region.toString());

        discoverSize(fileSystem, data, region);

        emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":fileSystem"), data));
      }),
      (noresp) -> logger.error("Failed to get fileSystem in {}", region)
    );
  }

  private void discoverSize(FileSystem resource, ObjectNode data, Region region) {
    try {
      List<Dimension> dimensions = new ArrayList<>();
      dimensions.add(Dimension.builder().name("FileSystemId").value(resource.fileSystemId()).build());
      Pair<Long, GetMetricStatisticsResponse> freeStorageCapacity =
        getCloudwatchMetricMinimum(region.toString(), "AWS/FSx", "FreeDataStorageCapacity", dimensions);

      long capacityAsBytes = Conversions.GibToBytes(resource.storageCapacity());

      data.put("freeDataStorageCapacity", freeStorageCapacity.getValue0());
      data.put("sizeInBytes", capacityAsBytes - freeStorageCapacity.getValue0());
      data.put("maxSizeInBytes", capacityAsBytes);

    } catch (Exception ignored) {
    }
  }
}
