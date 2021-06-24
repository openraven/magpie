package io.openraven.magpie.plugins.aws.discovery.services.disabled;

import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.plugins.aws.discovery.services.RedshiftDiscovery;
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

@Disabled // Disabled for current latest version of localstack 0.12.12. Not implemented yet
@ExtendWith(MockitoExtension.class)
public class RedshiftDiscoveryIT extends BaseAWSServiceIT {

  private final static String CF_REDSHIFT_TEMPLATE_PATH = "/template/redshift-template.yml";
  private final RedshiftDiscovery redshiftDiscovery = new RedshiftDiscovery();

  @Mock
  private Emitter emitter;

  @Captor
  private ArgumentCaptor<MagpieEnvelope> envelopeCapture;

  @BeforeAll
  public static void setup() {
    updateStackWithResources(CF_REDSHIFT_TEMPLATE_PATH);
  }

  @Test // NotImplementedError: The describe_storage action has not been implemented
  public void testRedshiftDiscovery() {
    // when
    redshiftDiscovery.discover(
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
