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
public class SecretsManagerDiscoveryIT extends BaseAWSServiceIT {

  private final String secretName = "TestSecret";
  private static final String CF_SECRET_TEMPLATE_PATH = "/template/secret-template.yml";
  private SecretsManagerDiscovery secretsManagerDiscovery = new SecretsManagerDiscovery();

  @Mock
  private Emitter emitter;

  @Captor
  private ArgumentCaptor<MagpieEnvelope> envelopeCapture;

  @BeforeAll
  public static void setup() {
    updateStackWithResources(CF_SECRET_TEMPLATE_PATH);
  }

  @Test
  public void testSecretManagerDiscovery() {
    // when
    secretsManagerDiscovery.discover(
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
    assertTrue(contents.get("assetId").asText().contains("secret:TestSecret"));
    assertEquals(secretName, contents.get("resourceName").asText());
    assertEquals("AWS::SecretsManager", contents.get("resourceType").asText());
    assertEquals(ACCOUNT, contents.get("accountId").asText());
    assertEquals(BASE_REGION.toString(), contents.get("region").asText());

    assertEquals("[{\"key\":\"AppName\",\"value\":\"OpenRavenIT\"}]",
      contents.get("configuration").get("tags").toString());
  }
}
