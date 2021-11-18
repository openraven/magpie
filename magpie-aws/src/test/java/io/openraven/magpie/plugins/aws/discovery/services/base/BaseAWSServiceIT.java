package io.openraven.magpie.plugins.aws.discovery.services.base;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.*;

import java.util.Objects;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.*;

public abstract class BaseAWSServiceIT {

  private static final int EXPOSED_EDGE_PORT = 4566;
  protected static final String EMPTY_STACK_TEMPLATE_PATH = "/template/empty-stack.yml";
  private static final String STACK_NAME = "integration-stack-" + System.nanoTime();

  protected static final Region BASE_REGION = Region.US_WEST_1;
  protected static final String ACCOUNT = "account";
  protected static final Session SESSION = new Session();
  protected static final Logger LOGGER = LoggerFactory.getLogger(BaseAWSServiceIT.class);
  protected static final long STACK_UPDATE_MILLS = 1000L;


  protected static final ObjectMapper MAPPER = new ObjectMapper()
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .findAndRegisterModules();

  protected static LocalStackContainer localStackContainer =
    new LocalStackContainer(DockerImageName.parse("localstack/localstack:0.13.0"))
      .withExposedPorts(EXPOSED_EDGE_PORT)
      .withEnv("DEFAULT_REGION", BASE_REGION.id())
      .withEnv("DEBUG", "1")
      .withServices(
        DYNAMODB, S3, CLOUDFORMATION, IAM, SNS, SECRETSMANAGER, ROUTE53, LAMBDA, EC2, KMS
      ); // At the moment cannot launch services dynamically

  private static CloudFormationClient cfClient;

  static {
    localStackContainer.start();
    setupEnvironment();
    initiateCloudFormationClient();
    startStackWithResources(EMPTY_STACK_TEMPLATE_PATH);
  }

  protected static void setupEnvironment() {
    System.getProperties().setProperty("aws.accessKeyId", "foo");
    System.getProperties().setProperty("aws.secretAccessKey", "bar");
    System.getProperties().setProperty("MAGPIE_AWS_ENDPOINT",
      String.format("http://%s:%d",
        localStackContainer.getHost(),
        localStackContainer.getMappedPort(EXPOSED_EDGE_PORT)));

    // Use local docker container. Comment localstack.start() as well
    // System.getProperties().setProperty("MAGPIE_AWS_ENDPOINT", "http://localhost:4566/");
  }

  protected static void initiateCloudFormationClient() {
    cfClient = AWSUtils.configure(CloudFormationClient.builder(), BASE_REGION);
  }

  protected static void startStackWithResources(String templatePath) {
    CreateStackRequest createStackRequest = CreateStackRequest.builder()
      .stackName(STACK_NAME)
      .templateBody(getResourceAsString(templatePath))
      .build();

    CreateStackResponse createdStack = cfClient.createStack(createStackRequest);
    assertNotNull(createdStack.stackId());
    LOGGER.info("Stack has been created with definition: {}", createdStack); // For transparency purpose
  }

  protected static void updateStackWithResources(String templatePath) {
    UpdateStackRequest updateStackRequest = UpdateStackRequest.builder()
      .stackName(STACK_NAME)
      .templateBody(getResourceAsString(templatePath))
      .build();

    UpdateStackResponse updateStackResponse = cfClient.updateStack(updateStackRequest);
    assertNotNull(updateStackResponse.stackId());

    LOGGER.info(cfClient.describeStacks(req -> req.stackName(STACK_NAME)).toString());

    cfClient.listStackResources(req -> req.stackName(STACK_NAME))
      .stackResourceSummaries()
      .forEach(stackResourceSummary -> LOGGER.info(stackResourceSummary.toString()));

    waitUpdateComplete(STACK_UPDATE_MILLS); // Drastically improve execution time. Below statement only for confirmation used
    cfClient.waiter().waitUntilStackUpdateComplete(req -> req.stackName(STACK_NAME));

    LOGGER.info(cfClient.describeStacks(req -> req.stackName(STACK_NAME)).toString());

    LOGGER.info("Stack has been updated with template: {}", templatePath);
  }

  protected static String getResourceAsString(String resourcePath) {
    return new Scanner(Objects.requireNonNull(BaseAWSServiceIT.class.getResourceAsStream(resourcePath)), "UTF-8")
      .useDelimiter("\\A").next();
  }

  private static void waitUpdateComplete(Long mills) {
    try {
      Thread.sleep(mills);
    } catch (InterruptedException e) {
      LOGGER.info("Wait stack update filed");
    }
  }
}
