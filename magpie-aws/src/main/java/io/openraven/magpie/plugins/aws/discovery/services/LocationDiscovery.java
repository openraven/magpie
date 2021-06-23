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
import io.openraven.magpie.api.MagpieResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.location.LocationClient;
import software.amazon.awssdk.services.location.model.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class LocationDiscovery implements AWSDiscovery {

  private static final String SERVICE = "location";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    // https://docs.aws.amazon.com/general/latest/gr/location.html
    return List.of(
      Region.AP_NORTHEAST_1,
      Region.AP_SOUTHEAST_1,
      Region.AP_SOUTHEAST_2,
      Region.EU_CENTRAL_1,
      Region.EU_WEST_1,
      Region.EU_NORTH_1,
      Region.US_EAST_1,
      Region.US_EAST_2,
      Region.US_WEST_2);
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    final var client = LocationClient.builder().region(region).build();

    discoverTrackers(mapper, session, region, emitter, account, client);
    discoverMaps(mapper, session, region, emitter, account, client);
    discoverGeofenceCollections(mapper, session, region, emitter, account, client);
    discoverPlaceIndex(mapper, session, region, emitter, account, client);
    discoverRouteCalculators(mapper, session, region, emitter, account, client);
  }

  private void discoverTrackers(ObjectMapper mapper, Session session, Region region, Emitter emitter, String account, LocationClient client) {
    final String RESOURCE_TYPE = "AWS::Location::Tracker";

    try {
      client.listTrackersPaginator(ListTrackersRequest.builder().build())
        .entries()
        .stream()
        .map(responseEntry -> client.describeTracker(DescribeTrackerRequest.builder().trackerName(responseEntry.trackerName()).build()))
        .forEach(tracker -> {
          var data = new MagpieResource.MagpieResourceBuilder(mapper, tracker.trackerArn())
            .withResourceName(tracker.trackerName())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(tracker.toBuilder()))
            .withCreatedIso(tracker.createTime())
            .withAccountId(account)
            .withRegion(region.toString())
            .build();

          discoverDevicePositions(client, tracker, data);
          discoverTrackerConsumers(client, tracker, data);
          discoverTags(client, tracker.trackerArn(), data, mapper);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":tracker"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverDevicePositions(LocationClient client, DescribeTrackerResponse tracker, MagpieResource data) {
    final String keyname = "devicePositions";

    getAwsResponse(
      () -> client.listDevicePositionsPaginator(ListDevicePositionsRequest.builder().trackerName(tracker.trackerName()).build()).entries()
        .stream()
        .map(ListDevicePositionsResponseEntry::toBuilder)
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverTrackerConsumers(LocationClient client, DescribeTrackerResponse tracker, MagpieResource data) {
    final String keyname = "trackerConsumers";

    getAwsResponse(
      () -> client.listTrackerConsumersPaginator(ListTrackerConsumersRequest.builder().trackerName(tracker.trackerName()).build()).consumerArns()
        .stream()
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverMaps(ObjectMapper mapper, Session session, Region region, Emitter emitter, String account, LocationClient client) {
    final String RESOURCE_TYPE = "AWS::Location::Map";

    try {
      client.listMapsPaginator(ListMapsRequest.builder().build())
        .entries()
        .stream()
        .map(responseEntry -> client.describeMap(DescribeMapRequest.builder().mapName(responseEntry.mapName()).build()))
        .forEach(map -> {
          var data = new MagpieResource.MagpieResourceBuilder(mapper, map.mapArn())
            .withResourceName(map.mapName())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(map.toBuilder()))
            .withCreatedIso(map.createTime())
            .withAccountId(account)
            .withRegion(region.toString())
            .build();

          discoverTags(client, map.mapArn(), data, mapper);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":map"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverGeofenceCollections(ObjectMapper mapper, Session session, Region region, Emitter emitter, String account, LocationClient client) {
    final String RESOURCE_TYPE = "AWS::Location::GeofenceCollection";

    try {
      client.listGeofenceCollectionsPaginator(ListGeofenceCollectionsRequest.builder().build())
        .entries()
        .stream()
        .map(responseEntry -> client.describeGeofenceCollection(DescribeGeofenceCollectionRequest.builder().collectionName(responseEntry.collectionName()).build()))
        .forEach(geofenceCollection -> {
          var data = new MagpieResource.MagpieResourceBuilder(mapper, geofenceCollection.collectionArn())
            .withResourceName(geofenceCollection.collectionName())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(geofenceCollection.toBuilder()))
            .withCreatedIso(geofenceCollection.createTime())
            .withAccountId(account)
            .withRegion(region.toString())
            .build();

          discoverGeofences(client, geofenceCollection, data);
          discoverTags(client, geofenceCollection.collectionArn(), data, mapper);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":geofenceCollection"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverGeofences(LocationClient client, DescribeGeofenceCollectionResponse geofenceCollection, MagpieResource data) {
    final String keyname = "geofences";

    getAwsResponse(
      () -> client.listGeofencesPaginator(ListGeofencesRequest.builder().collectionName(geofenceCollection.collectionName()).build()).entries()
        .stream()
        .map(ListGeofenceResponseEntry::toBuilder)
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverPlaceIndex(ObjectMapper mapper, Session session, Region region, Emitter emitter, String account, LocationClient client) {
    final String RESOURCE_TYPE = "AWS::Location::PlaceIndex";

    try {
      client.listPlaceIndexesPaginator(ListPlaceIndexesRequest.builder().build())
        .entries()
        .stream()
        .map(responseEntry -> client.describePlaceIndex(DescribePlaceIndexRequest.builder().indexName(responseEntry.indexName()).build()))
        .forEach(placeIndex -> {
          var data = new MagpieResource.MagpieResourceBuilder(mapper, placeIndex.indexArn())
            .withResourceName(placeIndex.indexName())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(placeIndex.toBuilder()))
            .withCreatedIso(placeIndex.createTime())
            .withAccountId(account)
            .withRegion(region.toString())
            .build();

          discoverTags(client, placeIndex.indexArn(), data, mapper);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":placeIndex"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverRouteCalculators(ObjectMapper mapper, Session session, Region region, Emitter emitter, String account, LocationClient client) {
    final String RESOURCE_TYPE = "AWS::Location::RouteCalculator";

    try {
      client.listRouteCalculatorsPaginator(ListRouteCalculatorsRequest.builder().build())
        .entries()
        .stream()
        .map(responseEntry -> client.describeRouteCalculator(DescribeRouteCalculatorRequest.builder().calculatorName(responseEntry.calculatorName()).build()))
        .forEach(routeCalculator -> {
          var data = new MagpieResource.MagpieResourceBuilder(mapper, routeCalculator.calculatorArn())
            .withResourceName(routeCalculator.calculatorName())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(routeCalculator.toBuilder()))
            .withCreatedIso(routeCalculator.createTime())
            .withAccountId(account)
            .withRegion(region.toString())
            .build();

          discoverTags(client, routeCalculator.calculatorArn(), data, mapper);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":routeCalculator"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverTags(LocationClient client, String arn, MagpieResource data, ObjectMapper mapper) {
    getAwsResponse(
      () -> client.listTagsForResource(ListTagsForResourceRequest.builder().resourceArn(arn).build()),
      (resp) -> data.tags = mapper.valueToTree(resp.tags()),
      (noresp) -> AWSUtils.update(data.tags, noresp)
    );
  }
}
