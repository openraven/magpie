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
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.javatuples.Pair;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.redshift.RedshiftClient;
import software.amazon.awssdk.services.redshift.model.Cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;
import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getCloudwatchDoubleMetricMaximum;

public class RedshiftDiscovery implements AWSDiscovery {

  private static final String SERVICE = "redshift";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return RedshiftClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger) {
    final var client = RedshiftClient.builder().region(region).build();

    getAwsResponse(
      () -> client.describeClustersPaginator().clusters().stream(),
      (resp) -> resp.forEach(cluster -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", cluster.toBuilder());
        data.put("region", region.toString());

        discoverStorage(client, data);
        discoverSize(cluster, data, region);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":cluster"), data));
      }),
      (noresp) -> logger.error("Failed to get clusters in {}", region)
    );
  }

  private void discoverStorage(RedshiftClient client, ObjectNode data) {
    final String keyname = "storage";

    getAwsResponse(
      client::describeStorage,
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private void discoverSize(Cluster resource, ObjectNode data, Region region) {
    try {
      List<Dimension> dimensions = new ArrayList<>();
      dimensions.add(Dimension.builder().name("ClusterIdentifier").value(resource.clusterIdentifier()).build());
      Pair<Double, GetMetricStatisticsResponse> percentageDiskSpaceUsed =
        getCloudwatchDoubleMetricMaximum(region.toString(), "AWS/Redshift", "PercentageDiskSpaceUsed", dimensions);

      data.put("PercentageDiskSpaceUsed", percentageDiskSpaceUsed.getValue0());

      // pull the relevant node(s) from the payload object. See https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/redshift.html
      JsonNode storageCapacityNode = data.at("storage/totalProvisionedStorageInMegaBytes");
      JsonNode usedPctNode = data.at("storage/PercentageDiskSpaceUsed");
      if (!storageCapacityNode.isMissingNode() && !usedPctNode.isMissingNode()) {
        long capacityAsBytes = storageCapacityNode.asLong() * 1049000L;
        @SuppressWarnings("WrapperTypeMayBePrimitive")
        Double dataUsed = (usedPctNode.asDouble() / 100) * capacityAsBytes;

        data.put("sizeInBytes", dataUsed.longValue());
        data.put("maxSizeInBytes", capacityAsBytes);
      }
    } catch (Exception ignored) {
    }
  }
}
