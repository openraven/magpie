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

package io.openraven.magpie.core.dmap.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.core.config.ConfigException;
import io.openraven.magpie.core.config.MagpieConfig;
import io.openraven.magpie.core.dmap.model.DMapTarget;
import io.openraven.magpie.core.dmap.model.EC2Target;
import io.openraven.magpie.core.dmap.client.DMapMLClient;
import io.openraven.magpie.core.dmap.client.DMapMLClientImpl;
import io.openraven.magpie.core.dmap.client.dto.AppProbability;
import io.openraven.magpie.core.dmap.dto.*;
import io.openraven.magpie.core.dmap.model.VpcConfig;
import io.openraven.magpie.plugins.persist.PersistConfig;
import io.openraven.magpie.plugins.persist.PersistPlugin;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkHttpClientBuilder;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.lang.String.join;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.stream.Collectors.*;

public class DMapServiceImpl implements DMapService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DMapServiceImpl.class);
  private static final String LAMBDA_NAME = "dmap-50b91f4405e9a8fd75692c2993e37102-296";
  private static final Duration TEN_MINUTES = Duration.ofMinutes(15);
  private static final String QUERY_TARGETS_SQL =   // TODO: save separately
    "SELECT t.resource_id, " +
    "       t.region,      " +
    "       t.configuration ->> 'subnetId' as subnet_id, " +
    "       t.configuration ->> 'privateIpAddress' as private_ip_address,  " +
    "       arr.group as security_group " +
    "FROM   assets t, LATERAL (" +
    "   SELECT string_agg(value::jsonb ->> 'groupId', ',') as group " +
    "   FROM   jsonb_array_elements_text(t.configuration->'securityGroups') " +
    "   ) arr " +
    "WHERE t.resource_type = 'AWS::EC2::Instance'";

  private final Jdbi jdbi;
  private final ObjectMapper mapper;
  private final DMapMLClient dMapMLClient;

  public DMapServiceImpl(MagpieConfig config) {
    mapper = new ObjectMapper();
    dMapMLClient = new DMapMLClientImpl(mapper);
    jdbi = initJdbiClient(config);
  }

  @Override
  public Map<VpcConfig, List<EC2Target>> groupScanTargets() {
    List<DMapTarget> scanTargets = jdbi.withHandle(handle -> handle.createQuery(QUERY_TARGETS_SQL)
      .map((rs, ctx) ->
        new DMapTarget(
          rs.getString("resource_id"),
          rs.getString("region"),
          rs.getString("subnet_id"),
          rs.getString("private_ip_address"),
          List.of(rs.getString("security_group").split(","))))
      .list());

    return scanTargets
      .stream()
      .collect(groupingBy(
        dmapTarget -> new VpcConfig(dmapTarget.getRegion(), dmapTarget.getSubnetId(), dmapTarget.getSecurityGroups()),
        mapping(dmapTarget -> new EC2Target(dmapTarget.getResourceId(), dmapTarget.getPrivateIpAddress()), toList())
      ));
  }


  @Override
  public DMapScanResult invokeLambda(Map<VpcConfig, List<EC2Target>> targets) {
    Instant startTime = Instant.now();

    List<FingerprintAnalysis> dmapAnalysis = new ArrayList<>();
    targets.forEach((vpcConfig, ec2Targets) -> {
      ec2Targets.forEach(ec2Target -> {
        LOGGER.info("Starting lambda for : {}", ec2Target);
        try (SdkHttpClient httpClient = getSdkHttpClient();
             LambdaClient lambdaClient = getLambdaClient(httpClient, vpcConfig.getRegion())) {

          InvokeRequest request = getLambdaRequest(ec2Target);
          InvokeResponse response = lambdaClient.invoke(request);

          DMapLambdaResponse dMapLambdaResponse = processResponse(vpcConfig, response);
          Map<String, DMapFingerprints> hosts = dMapLambdaResponse.getHosts();
          LOGGER.debug("Response from lambda {} is {}", LAMBDA_NAME, hosts);

          hosts.values().forEach(fingerprint -> {
            FingerprintAnalysis fingerprintAnalysis = new FingerprintAnalysis();
            fingerprintAnalysis.setResourceId(fingerprint.getId());
            fingerprintAnalysis.setRegion(vpcConfig.getRegion());
            fingerprintAnalysis.setAddress(fingerprint.getAddress());
            fingerprint.getSignatures().forEach((port, signature) -> {
              LOGGER.debug("Sending request to OpenRaven::DmapML service to analyze fingerprints by port: {}", port);
              List<AppProbability> predictions = dMapMLClient.predict(signature);
              fingerprintAnalysis.getPredictionsByPort().put(port, predictions);
            });
            dmapAnalysis.add(fingerprintAnalysis);
          });
        }
      });
    });
    LOGGER.debug("DMap predictions: {}", dmapAnalysis);

    Duration duration = Duration.between(startTime, Instant.now());
    return new DMapScanResult(dmapAnalysis, Date.from(startTime), duration);
  }

  private InvokeRequest getLambdaRequest(EC2Target ec2Target) {
    try {
      String payload = mapper.writeValueAsString(
        new DmapLambdaRequest(Map.of(ec2Target.getResourceId(), ec2Target.getIpAddress())));
      return InvokeRequest.builder()
        .functionName(LAMBDA_NAME)
        .payload(SdkBytes.fromUtf8String(payload))
        .invocationType(InvocationType.REQUEST_RESPONSE)
        .build();
    } catch (JsonProcessingException e) {
      LOGGER.error("Unable to serialize host data", e);
      throw new RuntimeException(e);
    }
  }

  private DMapLambdaResponse processResponse(VpcConfig vpcConfig, InvokeResponse response) {
    final String functionError = response.functionError();
    if (StringUtils.isNotBlank(functionError)) {
      String id = String.format("accountId=%s, subnet=%s, securityGroups=%s", "account",
        vpcConfig.getSubnetId(), join(",", vpcConfig.getSecurityGroupIds()));
      final String errorMessage = String.format("Non-null function error for %s: %s", id, functionError);
      throw new RuntimeException(errorMessage);
    }

    String payload = response.payload().asString(defaultCharset());
    try {
      return mapper.readValue(payload, DMapLambdaResponse.class);
    } catch (JsonProcessingException e) {
      LOGGER.info("Unable to parse response data from lambda: {}", payload, e);
      throw new RuntimeException(e);
    }
  }

  private LambdaClient getLambdaClient(SdkHttpClient httpClient, String region) {
    return LambdaClient.builder().region(Region.of(region))
      .httpClient(httpClient)
      .overrideConfiguration(
        ClientOverrideConfiguration
          .builder()
          .apiCallAttemptTimeout(TEN_MINUTES)
          .apiCallTimeout(TEN_MINUTES).build()
      ).build();
  }

  private SdkHttpClient getSdkHttpClient() {
    return new DefaultSdkHttpClientBuilder()
      .buildWithDefaults(
        AttributeMap.builder()
          .put(SdkHttpConfigurationOption.READ_TIMEOUT, TEN_MINUTES)
          .build());
  }

  private Jdbi initJdbiClient(MagpieConfig config) {
    final Jdbi jdbi;
    final var rawPersistConfig = config.getPlugins().get(PersistPlugin.ID);
    if (rawPersistConfig == null) {
      throw new ConfigException(String.format("Config file does not contain %s configuration", PersistPlugin.ID));
    }

    try {
      final PersistConfig persistConfig = mapper.treeToValue(mapper.valueToTree(rawPersistConfig.getConfig()), PersistConfig.class);

      String url = String.format("jdbc:postgresql://%s:%s/%s", persistConfig.getHostname(), persistConfig.getPort(), persistConfig.getDatabaseName());
      jdbi = Jdbi.create(url, persistConfig.getUser(), persistConfig.getPassword())
        .installPlugin(new PostgresPlugin());
    } catch (JsonProcessingException e) {
      throw new ConfigException("Cannot instantiate PersistConfig while initializing PolicyAnalyzerService", e);
    }
    return jdbi;
  }
}
