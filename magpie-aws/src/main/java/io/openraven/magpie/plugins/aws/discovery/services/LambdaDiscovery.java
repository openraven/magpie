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
import io.openraven.magpie.data.aws.lambda.LambdaFunction;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.EventSourceMappingConfiguration;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.FunctionEventInvokeConfig;
import software.amazon.awssdk.services.lambda.model.GetFunctionEventInvokeConfigRequest;
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest;
import software.amazon.awssdk.services.lambda.model.GetPolicyRequest;
import software.amazon.awssdk.services.lambda.model.ListEventSourceMappingsRequest;
import software.amazon.awssdk.services.lambda.model.ListFunctionEventInvokeConfigsRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class LambdaDiscovery implements AWSDiscovery {
  private static final String SERVICE = "lambda";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return LambdaClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {
    final String RESOURCE_TYPE = LambdaFunction.RESOURCE_TYPE;

    try (final var client = clientCreator.apply(LambdaClient.builder()).build()) {
      client.listFunctionsPaginator().functions().forEach(function -> {
        var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, function.functionArn())
          .withResourceName(function.functionName())
          .withResourceId(function.revisionId())
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(function.toBuilder()))
          .withAccountId(account)
          .withAwsRegion(region.toString())
          .build();

        discoverFunctionEventInvokeConfigs(client, function, data);
        discoverEventSourceMapping(client, function, data);
        discoverFunction(client, function, data);
        discoverFunctionInvokeConfig(client, function, data);
        discoverAccessPolicy(client, function, data);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":function"), data.toJsonNode()));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverFunctionEventInvokeConfigs(LambdaClient client, FunctionConfiguration resource, MagpieAwsResource data) {
    final String keyname = "EventInvokeConfigs";
    getAwsResponse(
      () -> client.listFunctionEventInvokeConfigsPaginator(ListFunctionEventInvokeConfigsRequest.builder().functionName(resource.functionName()).build()).functionEventInvokeConfigs()
        .stream()
        .map(FunctionEventInvokeConfig::toBuilder)
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverEventSourceMapping(LambdaClient client, FunctionConfiguration resource, MagpieAwsResource data) {
    final String keyname = "EventSourceMapping";
    getAwsResponse(
      () -> client.listEventSourceMappingsPaginator(ListEventSourceMappingsRequest.builder().functionName(resource.functionName()).build()).eventSourceMappings()
        .stream()
        .map(EventSourceMappingConfiguration::toBuilder)
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverFunctionInvokeConfig(LambdaClient client, FunctionConfiguration resource, MagpieAwsResource data) {
    final String keyname = "FunctionInvokeConfig";
    getAwsResponse(
      () -> client.getFunctionEventInvokeConfig(GetFunctionEventInvokeConfigRequest.builder().functionName(resource.functionName()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverFunction(LambdaClient client, FunctionConfiguration resource, MagpieAwsResource data) {
    final String keyname = "Function";
    getAwsResponse(
      () -> client.getFunction(GetFunctionRequest.builder().functionName(resource.functionName()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverAccessPolicy(LambdaClient client, FunctionConfiguration resource, MagpieAwsResource data) {
    final String keyname = "AccessPolicy";
    getAwsResponse(
      () -> client.getPolicy(GetPolicyRequest.builder().functionName(resource.functionName()).build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }
}
