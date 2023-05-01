package io.openraven.magpie.plugins.aws.discovery.services;

import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.plugins.aws.discovery.AWSDiscoveryConfig;
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
class S3DiscoveryIT extends BaseAWSServiceIT {

  private static final String BUCKET_NAME = "testbucket";
  private static final String CF_S3_TEMPLATE_PATH = "/template/s3-template.yml";

  private final S3Discovery s3Discovery = new S3Discovery();

  @Mock
  private Emitter emitter;

  @Captor
  private ArgumentCaptor<MagpieEnvelope> envelopeCapture;

  @BeforeAll
  public static void setup() {
    // given
    updateStackWithResources(CF_S3_TEMPLATE_PATH);
  }

  @Test
  public void testS3Discovery() {
    // when
    s3Discovery.discover(
      MAPPER,
      SESSION,
      BASE_REGION,
      emitter,
      LOGGER,
      ACCOUNT,
      ClientCreators.localClientCreator(BASE_REGION),
      new AWSDiscoveryConfig()
    );

    // then
    Mockito.verify(emitter).emit(envelopeCapture.capture());
    var contents = envelopeCapture.getValue().getContents();

    assertNotNull(contents.get("documentId"));
    assertEquals("arn:aws:s3:::testbucket", contents.get("arn").asText());
    assertEquals(BUCKET_NAME, contents.get("resourceName").asText());
    assertEquals("AWS::S3::Bucket", contents.get("resourceType").asText());
    assertEquals(ACCOUNT, contents.get("awsAccountId").asText());
    assertEquals(BASE_REGION.toString(), contents.get("awsRegion").asText());
  }
}



