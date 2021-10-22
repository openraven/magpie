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
import software.amazon.awssdk.services.elasticbeanstalk.ElasticBeanstalkClient;
import software.amazon.awssdk.services.elasticbeanstalk.model.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class EBDiscovery implements AWSDiscovery {

  private static final String SERVICE = "eb";


  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return ElasticBeanstalkClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {
    final var client = clientCreator.apply(ElasticBeanstalkClient.builder()).build();
    final String RESOURCE_TYPE = "AWS::ElasticBeanstalk";

    try {
      client.describeEnvironments().environments().forEach(environment -> {
        var data = new MagpieResource.MagpieResourceBuilder(mapper, environment.environmentArn())
          .withResourceName(environment.environmentName())
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(environment.toBuilder()))
          .withAccountId(account)
          .withRegion(region.toString())
          .build();

        discoverApplication(client, environment, data);
        discoverConfigurationSettings(client, environment, data);
        discoverEnvironmentResources(client, environment, data);
        discoverEnvironmentManagedActions(client, environment, data);
        discoverTags(client, environment, data, mapper);
        discoverBackupJobs(environment.environmentArn(), region, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":environment"), data.toJsonNode()));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverApplication(ElasticBeanstalkClient client, EnvironmentDescription resource, MagpieResource data) {
    final String keyname = "application";

    getAwsResponse(
      () -> client.describeApplications(
        DescribeApplicationsRequest.builder().applicationNames(resource.applicationName()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverConfigurationSettings(ElasticBeanstalkClient client, EnvironmentDescription resource, MagpieResource data) {
    final String keyname = "configurationSettings";

    getAwsResponse(
      () -> client.describeConfigurationSettings(
        DescribeConfigurationSettingsRequest.builder().
          applicationName(resource.applicationName()).
          environmentName(resource.environmentName()).
          build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverEnvironmentResources(ElasticBeanstalkClient client, EnvironmentDescription resource, MagpieResource data) {
    final String keyname = "environmentResources";

    getAwsResponse(
      () -> client.describeEnvironmentResources(DescribeEnvironmentResourcesRequest.builder().
        environmentName(resource.environmentName()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverEnvironmentManagedActions(ElasticBeanstalkClient client, EnvironmentDescription resource, MagpieResource data) {
    final String keyname = "environmentManagedActions";

    getAwsResponse(
      () -> client.describeEnvironmentManagedActions(DescribeEnvironmentManagedActionsRequest.builder().
        environmentName(resource.environmentName()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverTags(ElasticBeanstalkClient client, EnvironmentDescription resource, MagpieResource data, ObjectMapper mapper) {
    getAwsResponse(
      () -> client.listTagsForResource(ListTagsForResourceRequest.builder().resourceArn(resource.environmentArn()).build()),
      (resp) -> {
        JsonNode tagsNode = mapper.convertValue(resp.resourceTags().stream()
          .collect(Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
        AWSUtils.update(data.tags, tagsNode);
      },
      (noresp) -> AWSUtils.update(data.tags, noresp)
    );
  }
}
