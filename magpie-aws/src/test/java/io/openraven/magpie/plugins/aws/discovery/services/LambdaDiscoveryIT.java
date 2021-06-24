package io.openraven.magpie.plugins.aws.discovery.services;


import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.plugins.aws.discovery.services.base.BaseAWSServiceIT;
import io.openraven.magpie.plugins.aws.discovery.services.base.BaseIAMServiceIT;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class LambdaDiscoveryIT extends BaseIAMServiceIT {

  private static final String CF_LAMBDA_TEMPLATE_PATH = "/template/lambda-template.yml";
  private LambdaDiscovery lambdaDiscovery = new LambdaDiscovery();

  @Mock
  private Emitter emitter;

  @Captor
  private ArgumentCaptor<MagpieEnvelope> envelopeCapture;

  @BeforeAll
  public static void setup() {
    updateStackWithResources(CF_LAMBDA_TEMPLATE_PATH);
  }

  @AfterAll
  public static void cleanup() {
    removePolicies();
  }

  /**
   *  Issue with localstack response under the method client.getFunctionEventInvokeConfig
   *  Unable to parse date format. Hence fallback to simpler template
   */
  @Test
  public void testLambdaDiscovery() {
    // then
    lambdaDiscovery.discover(
      MAPPER,
      SESSION,
      BASE_REGION,
      emitter,
      LOGGER,
      ACCOUNT
    );

    // then
    Mockito.verify(emitter).emit(envelopeCapture.capture());
    var envelope = envelopeCapture.getValue();

    assertBase(envelope);
    assertConfiguration(envelope);
  }

  private void assertBase(MagpieEnvelope envelope) {
    var base = envelope.getContents();
    assertNotNull(base.get("documentId"));
    assertTrue(base.get("assetId").asText().contains("arn:aws:lambda:us-west-1:000000000000:function:integration-stack"));
    assertNotNull(base.get("resourceName").asText());
    assertNotNull(base.get("resourceId").asText());
    assertEquals("AWS::Lambda::Function", base.get("resourceType").asText());
    assertEquals(ACCOUNT, base.get("accountId").asText());
    assertEquals(BASE_REGION.toString(), base.get("region").asText());
  }

  private void assertConfiguration(MagpieEnvelope envelope) {
    var configuration = envelope.getContents().get("configuration");
    assertTrue(configuration.get("functionName").asText().contains("lambda"));
    assertTrue(configuration.get("functionArn").asText()
      .contains("arn:aws:lambda:us-west-1:000000000000:function:integration-stack"));
    assertEquals("nodejs12.x", configuration.get("runtime").asText());
    assertEquals("arn:aws:iam::000000000000:role/lambda-role", configuration.get("role").asText());
    assertEquals("index.handler", configuration.get("handler").asText());
    assertEquals("$LATEST", configuration.get("version").asText());
    assertEquals("Zip", configuration.get("packageType").asText());
  }

}
