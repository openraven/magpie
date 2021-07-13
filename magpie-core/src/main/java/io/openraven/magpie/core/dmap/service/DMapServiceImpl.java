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
import io.openraven.magpie.core.dmap.DMapProcessingException;
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
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.*;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

import static java.lang.String.join;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.stream.Collectors.*;

public class DMapServiceImpl implements DMapService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DMapServiceImpl.class);
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

  private static final String DMAP_LAMBDA_JAR_PATH = "lambda/source/dmap-lambda-0.0.0.jar";
  private static final int TIMEOUT = 900;
  private static final int MEMORY_SIZE = 512;

  private static final String ROLE_NAME = "openraven-dmap-scan-role";
  private static final String POLICY_NAME = "openraven-dmap-scan-policy";
  private static final String DMAP_LAMBDA_HANDLER = "io.openraven.dmap.lambda.Scan::handleRequest";

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

    try (IamClient iam = IamClient.builder().region(Region.AWS_GLOBAL).build()) {
      String lambdaRoleArn = createLambdaRole(iam);
      String policyArn = createPolicyAndAttachRole(iam);
      LOGGER.info("Role: {} with attached Policy: {} for Dmap-Lambda has been created\n", policyArn, lambdaRoleArn);

      targets.forEach((vpcConfig, ec2Targets) -> {

        try (SdkHttpClient httpClient = getSdkHttpClient();
             LambdaClient lambdaClient = getLambdaClient(httpClient, vpcConfig.getRegion())) {

          String lambdaName = createLambda(lambdaClient, vpcConfig, lambdaRoleArn);

          ec2Targets.forEach(ec2Target -> {
            LOGGER.info("Starting lambda: {} for : {}", lambdaName, ec2Target);

            InvokeRequest request = getLambdaRequest(lambdaName, ec2Target);
            InvokeResponse response = lambdaClient.invoke(request);

            DMapLambdaResponse dMapLambdaResponse = processResponse(vpcConfig, response);
            Map<String, DMapFingerprints> hosts = dMapLambdaResponse.getHosts();
            LOGGER.debug("Response from lambda {} is {}", lambdaName, hosts);

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
          });
          deleteLambda(lambdaClient, lambdaName);
        }
      });
      deleteLambdaRoleAndPolicy(iam, policyArn);
    }
    LOGGER.debug("DMap predictions: {}", dmapAnalysis);

    Duration duration = Duration.between(startTime, Instant.now());
    return new DMapScanResult(dmapAnalysis, Date.from(startTime), duration);
  }

  private void deleteLambdaRoleAndPolicy(IamClient iam, String policyArn) {
    iam.detachRolePolicy(builder -> builder.policyArn(policyArn).roleName(ROLE_NAME).build());
    iam.deletePolicy(builder -> builder.policyArn(policyArn));
    iam.deleteRole(builder -> builder.roleName(ROLE_NAME).build());
    LOGGER.info("Role: {} and attached policy: {} has been deleted", ROLE_NAME, policyArn);
  }

  private String createLambdaRole(IamClient iam) {
    try {

      GetRoleResponse response = iam.getRole(builder -> builder.roleName(ROLE_NAME).build());
      LOGGER.info("Required role: {} was found reusing its ARN: {}", ROLE_NAME, response.role().arn());
      return response.role().arn();

    } catch (NoSuchEntityException e) {
      LOGGER.info("Creating {}", ROLE_NAME);
      CreateRoleRequest request = CreateRoleRequest.builder()
        .roleName(ROLE_NAME)
        .assumeRolePolicyDocument(getResourceAsString("/lambda/policy/assume-role-policy.json"))
        .description("Role lambda invoking for DMAP port scanning over EC2 services")
        .build();
      CreateRoleResponse response = iam.createRole(request);
      iam.waiter().waitUntilRoleExists(builder -> builder.roleName(ROLE_NAME).build());
      return response.role().arn();
    }
  }

  private String createPolicyAndAttachRole(IamClient iam) {
    List<AttachedPolicy> attachedPolicies =
      iam.listAttachedRolePolicies(builder -> builder.roleName(ROLE_NAME)).attachedPolicies();

    if (attachedPolicies.isEmpty()) {
      CreatePolicyResponse policyResponse = iam.createPolicy(builder -> builder
        .policyName(POLICY_NAME)
        .policyDocument(getResourceAsString("/lambda/policy/role-policy.json"))
        .build()
      );

      String policyArn = policyResponse.policy().arn();
      iam.attachRolePolicy(builder ->
        builder
          .policyArn(policyArn)
          .roleName(ROLE_NAME)
          .build());
      iam.waiter().waitUntilPolicyExists(builder -> builder.policyArn(policyArn).build());
      return policyArn;
    }
    LOGGER.info("Skipped policy creation. Target role found with attached policies: {}", attachedPolicies);
    return attachedPolicies.stream()
      .filter(attachedPolicy -> attachedPolicy.policyName().equals(POLICY_NAME))
      .findFirst()
      .map(AttachedPolicy::policyArn)
      .orElseThrow(() ->
        new DMapProcessingException("Incomplete state of Role: " + ROLE_NAME + " Policy: " + POLICY_NAME));
  }

  public String createLambda(LambdaClient lambdaClient,
                             VpcConfig vpcConfig,
                             String roleArn) {
    LOGGER.info("Creating lambda function in VPC: {} ", vpcConfig);

    SdkBytes source = Optional.ofNullable(this.getClass().getClassLoader()
      .getResourceAsStream(DMAP_LAMBDA_JAR_PATH))
      .map(SdkBytes::fromInputStream)
      .orElseThrow(() -> new RuntimeException("Unable to find sources under " + DMAP_LAMBDA_JAR_PATH));

    CreateFunctionRequest functionRequest = CreateFunctionRequest.builder()
      .functionName("dmap-" + UUID.randomUUID())
      .description("DMAP Lambda function for port scanning distributed by OpenRaven")
      .code(builder -> builder.zipFile(source).build())
      .handler(DMAP_LAMBDA_HANDLER)
      .runtime(Runtime.JAVA11)
      .timeout(TIMEOUT)
      .memorySize(MEMORY_SIZE)
      .role(roleArn)
      .vpcConfig(builder -> builder
        .securityGroupIds(vpcConfig.getSecurityGroupIds())
        .subnetIds(vpcConfig.getSubnetId())
        .build())
      .build();

    CreateFunctionResponse functionResponse = lambdaClient.createFunction(functionRequest);
    lambdaClient.waiter()
      .waitUntilFunctionActive(builder -> builder.functionName(functionResponse.functionName()).build());
    LOGGER.info("Lambda function for DMap port scan has been created: {}", functionResponse.functionName());
    return functionResponse.functionName();
  }

  private void deleteLambda(LambdaClient lambdaClient, String lambdaName) {
    lambdaClient.deleteFunction(DeleteFunctionRequest.builder().functionName(lambdaName).build());
    LOGGER.info("Lambda function: {} has been removed\n", lambdaName);
  }

  private InvokeRequest getLambdaRequest(String lambdaName, EC2Target ec2Target) {
    try {
      String payload = mapper.writeValueAsString(
        new DmapLambdaRequest(Map.of(ec2Target.getResourceId(), ec2Target.getIpAddress())));
      return InvokeRequest.builder()
        .functionName(lambdaName)
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

  private static String getResourceAsString(String resourcePath) {
    return new Scanner(
      Objects.requireNonNull(DMapServiceImpl.class.getResourceAsStream(resourcePath)),
      StandardCharsets.UTF_8)
      .useDelimiter("\\A").next();
  }
}
