package io.openraven.magpie.plugins.aws.discovery.services.disabled;

import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.plugins.aws.discovery.services.KMSDiscovery;
import io.openraven.magpie.plugins.aws.discovery.services.base.BaseAWSServiceIT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled // listKeyPolicies are failing on current version of localstack 0.12.12 (code 501)
@ExtendWith(MockitoExtension.class)
public class KMSDiscoveryIT extends BaseAWSServiceIT {

  private static final String CF_KMS_TEMPLATE_PATH = "/template/kms-template.yml";
  private KMSDiscovery kmsDiscovery = new KMSDiscovery();

  @Mock
  private Emitter emitter;

  @Captor
  private ArgumentCaptor<MagpieEnvelope> envelopeCapture;

  @BeforeAll
  public static void setup() {
    updateStackWithResources(CF_KMS_TEMPLATE_PATH);
  }

  @Test
  public void testKMSDiscovery() {
    kmsDiscovery.discover(
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
  }

}
