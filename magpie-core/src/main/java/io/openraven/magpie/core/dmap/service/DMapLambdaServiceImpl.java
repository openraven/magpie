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
import io.openraven.magpie.core.dmap.DMapExecutionContext;
import io.openraven.magpie.core.dmap.exception.DMapProcessingException;
import io.openraven.magpie.core.dmap.model.EC2Target;
import io.openraven.magpie.core.dmap.client.DMapMLClient;
import io.openraven.magpie.core.dmap.client.DMapMLClientImpl;
import io.openraven.magpie.core.dmap.client.dto.AppProbability;
import io.openraven.magpie.core.dmap.dto.*;
import io.openraven.magpie.core.dmap.model.VpcConfig;
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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.openraven.magpie.core.dmap.Util.getResourceAsString;
import static java.lang.String.join;
import static java.nio.charset.Charset.defaultCharset;

public class DMapLambdaServiceImpl implements DMapLambdaService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DMapLambdaServiceImpl.class);
  private static final Duration FIFTEEN_MINUTES = Duration.ofMinutes(15);

  private static final String DMAP_LAMBDA_JAR_PATH = "lambda/source/dmap-lambda-0.0.0.jar";
  private static final String ROLE_NAME = "openraven-dmap-scan-role";
  private static final String POLICY_NAME = "openraven-dmap-scan-policy";
  private static final String DMAP_LAMBDA_HANDLER = "io.openraven.dmap.lambda.Scan::handleRequest";

  private static final int TIMEOUT = 900;
  private static final int MEMORY_SIZE = 512;
  private static final int SYNC_TIMEOUT = 9000;
  private static final int TARGET_POOL_MULTIPLIER = 2;

  private final ObjectMapper mapper;
  private final DMapMLClient dMapMLClient;
  private final ExecutorService lambdaExecutorService;
  private final ExecutorService ec2targetExecutorService;

  public DMapLambdaServiceImpl(int workers) {
    mapper = new ObjectMapper();
    dMapMLClient = new DMapMLClientImpl(mapper);
    lambdaExecutorService = Executors.newFixedThreadPool(workers);
    ec2targetExecutorService = Executors.newFixedThreadPool(workers * TARGET_POOL_MULTIPLIER);
  }

  @Override
  public DMapScanResult startDMapScan(Map<VpcConfig, List<EC2Target>> vpcGroups,
                                      DMapExecutionContext dMapExecutionContext) {
    Instant startTime = Instant.now();

    String roleArn = createRequiredRole();
    List<FingerprintAnalysis> fingerprintAnalysis = analyzeTargetSerivces(vpcGroups, roleArn, dMapExecutionContext);

    LOGGER.debug("DMap predictions: {}", fingerprintAnalysis);

    Duration duration = Duration.between(startTime, Instant.now());
    return new DMapScanResult(fingerprintAnalysis, Date.from(startTime), duration);
  }

  @Override
  public void cleanupCreatedResources(DMapExecutionContext dMapExecutionContext) {
    LOGGER.info("Cleanup created resources after DMap Lambda execution");

    try (IamClient iam = IamClient.builder().region(Region.AWS_GLOBAL).build()) {
      List<AttachedPolicy> attachedPolicies =
        iam.listAttachedRolePolicies(builder -> builder.roleName(ROLE_NAME)).attachedPolicies();

      // Drop policies
      attachedPolicies.forEach(attachedPolicy -> {
          iam.detachRolePolicy(builder -> builder.roleName(ROLE_NAME).policyArn(attachedPolicy.policyArn()));
          iam.deletePolicy(builder -> builder.policyArn(attachedPolicy.policyArn()));
          LOGGER.info("Policy: {} has been removed", attachedPolicy.policyArn());
        }
      );

      // Drop roles
      iam.deleteRole(builder -> builder.roleName(ROLE_NAME).build());
      LOGGER.info("Role: {} has been removed", ROLE_NAME);

    } catch (NoSuchEntityException e) {
      LOGGER.info("DMap Lambda related resources not found. Assume stack clean");
      LOGGER.debug("Exception: ", e);
    }

    // Drop Lambda
    if (dMapExecutionContext.getRegion() != null && dMapExecutionContext.getLambdaName() != null) {
      try (LambdaClient lambdaClient =
             LambdaClient.builder().region(Region.of(dMapExecutionContext.getRegion())).build()) {
        deleteLambda(lambdaClient, dMapExecutionContext.getLambdaName());
      } catch (Exception e) {
        LOGGER.warn("Unable to delete lambda: {}", dMapExecutionContext.getLambdaName());
      }
    }
  }

  private String createRequiredRole() {
    try (IamClient iam = IamClient.builder().region(Region.AWS_GLOBAL).build()) {
      String lambdaRoleArn = createLambdaRole(iam);
      String policyArn = createPolicyAndAttachRole(iam);
      waitForRoleFinalization();
      LOGGER.info("Role: {} with attached Policy: {} for Dmap-Lambda has been created\n", policyArn, lambdaRoleArn);
      return lambdaRoleArn;
    }
  }

  private List<FingerprintAnalysis> analyzeTargetSerivces(Map<VpcConfig, List<EC2Target>> vpcGroups,
                                                          String lambdaRoleArn,
                                                          DMapExecutionContext dMapExecutionContext) {
    List<FingerprintAnalysis> dmapAnalysis = new ArrayList<>();

    var lambdaCountDownLatch = new CountDownLatch(vpcGroups.size()); // Executions per VPC
    vpcGroups.forEach((vpcConfig, ec2Targets) -> {

      lambdaExecutorService.submit(() -> {

        try (SdkHttpClient httpClient = getSdkHttpClient();
             LambdaClient lambdaClient = getLambdaClient(httpClient, vpcConfig.getRegion())) {

          String lambdaName = createLambda(lambdaClient, vpcConfig, lambdaRoleArn);
          // TODO : map of regions and lambdas set
          dMapExecutionContext.setRegion(vpcConfig.getRegion());
          dMapExecutionContext.setLambdaName(lambdaName);

          var ec2TargetCountDownLatch = new CountDownLatch(ec2Targets.size());
          ec2Targets.forEach(ec2Target -> {

            ec2targetExecutorService.submit(() -> {
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
              ec2TargetCountDownLatch.countDown();
            });

          });
          waitForCompletion(ec2TargetCountDownLatch);
          deleteLambda(lambdaClient, lambdaName);
          dMapExecutionContext.clear();
        }
        lambdaCountDownLatch.countDown();
      });
    });
    waitForCompletion(lambdaCountDownLatch);

    ec2targetExecutorService.shutdown();
    lambdaExecutorService.shutdown();

    return dmapAnalysis;
  }

  private void waitForCompletion(CountDownLatch countDownLatch) {
    try {
      countDownLatch.await();
    } catch (InterruptedException e) {
      LOGGER.warn("DMap scan process was interrupted");
    }
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
      iam.waiter().waitUntilPolicyExists(builder -> builder.policyArn(policyArn).build());
      iam.attachRolePolicy(builder ->
        builder
          .policyArn(policyArn)
          .roleName(ROLE_NAME)
          .build());
      return policyArn;
    }
    
    LOGGER.info("Skipped policy creation. Target role found with attached policies: {}", attachedPolicies);
    return attachedPolicies.stream()
      .filter(attachedPolicy -> attachedPolicy.policyName().equals(POLICY_NAME))
      .findFirst()
      .map(AttachedPolicy::policyArn)
      .orElseThrow(() ->
        new DMapProcessingException("Incomplete state of Role: " + ROLE_NAME + " Policy: " + POLICY_NAME + " not found"));
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
      throw new DMapProcessingException(errorMessage);
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
          .apiCallAttemptTimeout(FIFTEEN_MINUTES)
          .apiCallTimeout(FIFTEEN_MINUTES).build()
      ).build();
  }

  private SdkHttpClient getSdkHttpClient() {
    return new DefaultSdkHttpClientBuilder()
      .buildWithDefaults(
        AttributeMap.builder()
          .put(SdkHttpConfigurationOption.READ_TIMEOUT, FIFTEEN_MINUTES)
          .build());
  }
  
  private void waitForRoleFinalization() {
    try {
      LOGGER.info("Waiting for role state finalization");
      Thread.sleep(SYNC_TIMEOUT);
    } catch (InterruptedException e) {
      LOGGER.warn("Finalization has been interrupted");
    }
  }
}
