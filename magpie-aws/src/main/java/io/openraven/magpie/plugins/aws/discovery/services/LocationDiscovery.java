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
import io.openraven.magpie.api.MagpieAwsResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.aws.location.LocationGeofenceCollection;
import io.openraven.magpie.data.aws.location.LocationMap;
import io.openraven.magpie.data.aws.location.LocationPlaceIndex;
import io.openraven.magpie.data.aws.location.LocationRouteCalculator;
import io.openraven.magpie.data.aws.location.LocationTracker;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.location.LocationClient;
import software.amazon.awssdk.services.location.model.DescribeGeofenceCollectionRequest;
import software.amazon.awssdk.services.location.model.DescribeGeofenceCollectionResponse;
import software.amazon.awssdk.services.location.model.DescribeMapRequest;
import software.amazon.awssdk.services.location.model.DescribePlaceIndexRequest;
import software.amazon.awssdk.services.location.model.DescribeRouteCalculatorRequest;
import software.amazon.awssdk.services.location.model.DescribeTrackerRequest;
import software.amazon.awssdk.services.location.model.DescribeTrackerResponse;
import software.amazon.awssdk.services.location.model.ListDevicePositionsRequest;
import software.amazon.awssdk.services.location.model.ListDevicePositionsResponseEntry;
import software.amazon.awssdk.services.location.model.ListGeofenceCollectionsRequest;
import software.amazon.awssdk.services.location.model.ListGeofenceResponseEntry;
import software.amazon.awssdk.services.location.model.ListGeofencesRequest;
import software.amazon.awssdk.services.location.model.ListMapsRequest;
import software.amazon.awssdk.services.location.model.ListPlaceIndexesRequest;
import software.amazon.awssdk.services.location.model.ListRouteCalculatorsRequest;
import software.amazon.awssdk.services.location.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.location.model.ListTrackerConsumersRequest;
import software.amazon.awssdk.services.location.model.ListTrackersRequest;

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
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {

    try (final var client = clientCreator.apply(LocationClient.builder()).build()) {
      discoverTrackers(mapper, session, region, emitter, account, client);
      discoverMaps(mapper, session, region, emitter, account, client);
      discoverGeofenceCollections(mapper, session, region, emitter, account, client);
      discoverPlaceIndex(mapper, session, region, emitter, account, client);
      discoverRouteCalculators(mapper, session, region, emitter, account, client);
    }
  }

  private void discoverTrackers(ObjectMapper mapper, Session session, Region region, Emitter emitter, String account, LocationClient client) {
    final String RESOURCE_TYPE = LocationTracker.RESOURCE_TYPE;

    try {
      client.listTrackersPaginator(ListTrackersRequest.builder().build())
        .entries()
        .stream()
        .map(responseEntry -> client.describeTracker(DescribeTrackerRequest.builder().trackerName(responseEntry.trackerName()).build()))
        .forEach(tracker -> {
          var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, tracker.trackerArn())
            .withResourceName(tracker.trackerName())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(tracker.toBuilder()))
            .withCreatedIso(tracker.createTime())
            .withAccountId(account)
            .withAwsRegion(region.toString())
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

  private void discoverDevicePositions(LocationClient client, DescribeTrackerResponse tracker, MagpieAwsResource data) {
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

  private void discoverTrackerConsumers(LocationClient client, DescribeTrackerResponse tracker, MagpieAwsResource data) {
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
    final String RESOURCE_TYPE = LocationMap.RESOURCE_TYPE;

    try {
      client.listMapsPaginator(ListMapsRequest.builder().build())
        .entries()
        .stream()
        .map(responseEntry -> client.describeMap(DescribeMapRequest.builder().mapName(responseEntry.mapName()).build()))
        .forEach(map -> {
          var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, map.mapArn())
            .withResourceName(map.mapName())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(map.toBuilder()))
            .withCreatedIso(map.createTime())
            .withAccountId(account)
            .withAwsRegion(region.toString())
            .build();

          discoverTags(client, map.mapArn(), data, mapper);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":map"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverGeofenceCollections(ObjectMapper mapper, Session session, Region region, Emitter emitter, String account, LocationClient client) {
    final String RESOURCE_TYPE = LocationGeofenceCollection.RESOURCE_TYPE;

    try {
      client.listGeofenceCollectionsPaginator(ListGeofenceCollectionsRequest.builder().build())
        .entries()
        .stream()
        .map(responseEntry -> client.describeGeofenceCollection(DescribeGeofenceCollectionRequest.builder().collectionName(responseEntry.collectionName()).build()))
        .forEach(geofenceCollection -> {
          var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, geofenceCollection.collectionArn())
            .withResourceName(geofenceCollection.collectionName())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(geofenceCollection.toBuilder()))
            .withCreatedIso(geofenceCollection.createTime())
            .withAccountId(account)
            .withAwsRegion(region.toString())
            .build();

          discoverGeofences(client, geofenceCollection, data);
          discoverTags(client, geofenceCollection.collectionArn(), data, mapper);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":geofenceCollection"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverGeofences(LocationClient client, DescribeGeofenceCollectionResponse geofenceCollection, MagpieAwsResource data) {
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
    final String RESOURCE_TYPE = LocationPlaceIndex.RESOURCE_TYPE;

    try {
      client.listPlaceIndexesPaginator(ListPlaceIndexesRequest.builder().build())
        .entries()
        .stream()
        .map(responseEntry -> client.describePlaceIndex(DescribePlaceIndexRequest.builder().indexName(responseEntry.indexName()).build()))
        .forEach(placeIndex -> {
          var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, placeIndex.indexArn())
            .withResourceName(placeIndex.indexName())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(placeIndex.toBuilder()))
            .withCreatedIso(placeIndex.createTime())
            .withAccountId(account)
            .withAwsRegion(region.toString())
            .build();

          discoverTags(client, placeIndex.indexArn(), data, mapper);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":placeIndex"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverRouteCalculators(ObjectMapper mapper, Session session, Region region, Emitter emitter, String account, LocationClient client) {
    final String RESOURCE_TYPE = LocationRouteCalculator.RESOURCE_TYPE;

    try {
      client.listRouteCalculatorsPaginator(ListRouteCalculatorsRequest.builder().build())
        .entries()
        .stream()
        .map(responseEntry -> client.describeRouteCalculator(DescribeRouteCalculatorRequest.builder().calculatorName(responseEntry.calculatorName()).build()))
        .forEach(routeCalculator -> {
          var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, routeCalculator.calculatorArn())
            .withResourceName(routeCalculator.calculatorName())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(routeCalculator.toBuilder()))
            .withCreatedIso(routeCalculator.createTime())
            .withAccountId(account)
            .withAwsRegion(region.toString())
            .build();

          discoverTags(client, routeCalculator.calculatorArn(), data, mapper);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":routeCalculator"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverTags(LocationClient client, String arn, MagpieAwsResource data, ObjectMapper mapper) {
    getAwsResponse(
      () -> client.listTagsForResource(ListTagsForResourceRequest.builder().resourceArn(arn).build()),
      (resp) -> data.tags = mapper.valueToTree(resp.tags()),
      (noresp) -> AWSUtils.update(data.tags, noresp)
    );
  }
}
