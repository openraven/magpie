package io.openraven.magpie.plugins.aws.discovery.services;


import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.plugins.aws.discovery.services.base.BaseAWSServiceIT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class SNSDiscoveryIT extends BaseAWSServiceIT {
  private static final String CF_SNS_TEMPLATE = "/template/sns-subscription-template.yml";
  private final SNSDiscovery snsDiscovery = new SNSDiscovery();

  @Mock
  private Emitter emitter;

  @Captor
  private ArgumentCaptor<MagpieEnvelope> envelopeCapture;

  @BeforeAll
  public static void setup() {
    updateStackWithResources(CF_SNS_TEMPLATE);
  }

  @Test
  public void testSNSDiscovery() {
    snsDiscovery.discover(
      MAPPER,
      SESSION,
      BASE_REGION,
      emitter,
      LOGGER,
      ACCOUNT
    );

    // Verify
    Mockito.verify(emitter, times(2)).emit(envelopeCapture.capture());
    var invocations = envelopeCapture.getAllValues();
    assertEquals(2, invocations.size());
    assertTopic(invocations.get(0));
    assertSubscription(invocations.get(1));
  }

  private void assertTopic(MagpieEnvelope envelope) {
    var content = envelope.getContents();
    assertNotNull(content.get("documentId"));
    assertTrue(content.get("assetId").asText().contains("arn:aws:sns:us-west-1:000000000000:CarSalesTopic"));
    assertEquals("AWS::SNS::Topic", content.get("resourceType").asText());
    assertEquals(BASE_REGION.toString(), content.get("region").asText());
  }

  private void assertSubscription(MagpieEnvelope envelope) {
    var content = envelope.getContents();
    assertNotNull(content.get("documentId"));
    assertNotNull(content.get("resourceName"));
    assertTrue(content.get("assetId").asText().contains("arn:aws:sns:us-west-1:000000000000:CarSalesTopic"));
    assertEquals("AWS::SNS::Subscription", content.get("resourceType").asText());
    assertEquals("test@openraven.com", content.get("configuration").get("attributes").get("Endpoint").asText());
    assertTrue(content.get("configuration").get("attributes").get("TopicArn").asText().contains("arn:aws:sns:us-west-1:000000000000:CarSalesTopic"));
    assertEquals(BASE_REGION.toString(), content.get("region").asText());
  }
}
