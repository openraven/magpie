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
import io.openraven.magpie.data.aws.athena.AthenaDataCatalog;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.DataCatalogSummary;
import software.amazon.awssdk.services.athena.model.ListDataCatalogsRequest;
import software.amazon.awssdk.services.athena.model.ListDatabasesRequest;
import software.amazon.awssdk.services.athena.model.ListDatabasesResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;
import static java.lang.String.format;

public class AthenaDiscovery implements AWSDiscovery {

  private static final String SERVICE = "athena";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return AthenaClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    final var client = AWSUtils.configure(AthenaClient.builder(), region);
    final String RESOURCE_TYPE = AthenaDataCatalog.RESOURCE_TYPE;

    try {
      client.listDataCatalogsPaginator(ListDataCatalogsRequest.builder().build()).dataCatalogsSummary()
        .forEach(dataCatalog -> {
          var arn = format("arn:aws:athena:%s:%s:datacatalog/%s", region, account, dataCatalog.catalogName());
          var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, arn)
            .withResourceName(dataCatalog.catalogName())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(dataCatalog.toBuilder()))
            .withAccountId(account)
            .withAwsRegion(region.toString())
            .build();

          discoverDatabases(client, dataCatalog, data);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":dataCatalog"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverDatabases(AthenaClient client, DataCatalogSummary resource, MagpieAwsResource data) {
    final String keyname = "databases";

    getAwsResponse(
      () -> client.listDatabasesPaginator(ListDatabasesRequest.builder().catalogName(resource.catalogName()).build())
        .stream()
        .map(ListDatabasesResponse::toBuilder)
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }
}
