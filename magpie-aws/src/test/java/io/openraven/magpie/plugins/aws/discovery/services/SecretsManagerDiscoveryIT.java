package io.openraven.magpie.plugins.aws.discovery.services;

import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;

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
      ACCOUNT
    );

    // then
    Mockito.verify(emitter).emit(envelopeCapture.capture());
    var contents = envelopeCapture.getValue().getContents();

    assertNotNull(contents.get("documentId"));
    assertTrue(contents.get("arn").asText().contains("secret:TestSecret"));
    assertEquals(secretName, contents.get("resourceName").asText());
    assertEquals("AWS::SecretsManager", contents.get("resourceType").asText());
    assertEquals("account", contents.get("awsAccountId").asText());
    assertEquals(BASE_REGION.toString(), contents.get("awsRegion").asText());

    assertEquals("[{\"key\":\"AppName\",\"value\":\"OpenRavenIT\"}]",
      contents.get("configuration").get("tags").toString());
  }

  private void createSecret(SecretsManagerClient client) {
    client.createSecret(req -> req
      .name("TestSecret")
      .secretString("{\"username\":\"TestUser\",\"password\":\"secret-password\"}"));
  }
}
