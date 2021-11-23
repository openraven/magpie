package io.openraven.magpie.plugins.aws.discovery.services;

import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.plugins.aws.discovery.ClientCreators;
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

@ExtendWith(MockitoExtension.class)
public class Route53DiscoveryIT extends BaseAWSServiceIT {

  private static final String CF_ROUTE53_TEMPLATE_PATH = "/template/route53-template.yml";
  private final Route53Discovery route53Discovery = new Route53Discovery();

  @Mock
  private Emitter emitter;

  @Captor
  private ArgumentCaptor<MagpieEnvelope> envelopeCapture;

  @BeforeAll
  public static void setup() {
    // given
    updateStackWithResources(CF_ROUTE53_TEMPLATE_PATH);
  }

  @Test
  public void testRoute53Discovery() {
    // when
    route53Discovery.discover(
      MAPPER,
      SESSION,
      BASE_REGION,
      emitter,
      LOGGER,
      ACCOUNT,
      ClientCreators.localClientCreator(BASE_REGION)
    );

    // then
    Mockito.verify(emitter).emit(envelopeCapture.capture());
    var contents = envelopeCapture.getValue().getContents();

    assertNotNull(contents.get("documentId"));
    assertTrue(contents.get("arn").asText().contains("arn:aws:route53:::hostedZone"));
    assertEquals("example.com", contents.get("resourceName").asText());
    assertEquals("AWS::Route53::HostedZone", contents.get("resourceType").asText());
    assertEquals(ACCOUNT, contents.get("awsAccountId").asText());
    assertEquals(BASE_REGION.toString(), contents.get("awsRegion").asText());
  }
}
