package io.openraven.magpie.plugins.aws.discovery.services;

import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.api.MagpieResource;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.BackupUtils;
import io.openraven.magpie.plugins.aws.discovery.ClientCreators;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.services.base.BaseAWSServiceIT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.ListBackupJobsRequest;
import software.amazon.awssdk.services.backup.model.ListBackupJobsResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamoDbDiscoveryIT extends BaseAWSServiceIT {

  private static final String TEST_TABLE = "entities";
  private static final String CF_DYNAMODB_TEMPLATE_PATH = "/template/dynamo-db-template.yml";

  private final DynamoDbDiscovery dynamoDbDiscovery = new DynamoDbDiscovery() {
    // We override this to make it a no-op since we can't perform Backup calls on the free version of Localstack.
    public void discoverBackupJobs(String arn, Region region, MagpieResource data, MagpieAWSClientCreator clientCreator) {}
  };

  @Mock
  private Emitter emitter;

  @Captor
  private ArgumentCaptor<MagpieEnvelope> envelopeCapture;

  @BeforeAll
  public static void setup() {
    updateStackWithResources(CF_DYNAMODB_TEMPLATE_PATH);
  }

  @Test
  public void testDynamoDBDiscovery() {
    // given
    DynamoDbClient dynamoDbClient = ClientCreators.localClientCreator(BASE_REGION).apply(DynamoDbClient.builder()).build();

    // when
    dynamoDbDiscovery.discoverTables(
      MAPPER,
      SESSION,
      BASE_REGION,
      emitter,
      dynamoDbClient,
      ACCOUNT,
      ClientCreators.localClientCreator(BASE_REGION)
    );

    // then
    Mockito.verify(emitter).emit(envelopeCapture.capture());
    var contents = envelopeCapture.getValue().getContents();

    assertNotNull(contents.get("documentId"));
    assertEquals(String.format("arn:aws:dynamodb:%s:000000000000:table/%s", BASE_REGION, TEST_TABLE),
      contents.get("arn").asText());
    assertEquals(TEST_TABLE, contents.get("resourceName").asText());
    assertEquals("AWS::DynamoDB::Table", contents.get("resourceType").asText());
    assertEquals(BASE_REGION.toString(), contents.get("awsRegion").asText());
  }
}
