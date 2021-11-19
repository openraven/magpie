package io.openraven.magpie.plugins.aws.discovery.services.disabled;

import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.ClientCreators;
import io.openraven.magpie.plugins.aws.discovery.services.CloudWatchDiscovery;
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
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled // Alarm history is not implemented - localstack 0.12.12. Unable to execute discovery.
@ExtendWith(MockitoExtension.class)
public class CloudWatchDiscoveryIT extends BaseAWSServiceIT {

  private static final String CF_CLOUDWATCH_TEMPLATE_PATH = "/template/cloudwatch-template.yml";
  private CloudWatchDiscovery cloudWatchDiscovery = new CloudWatchDiscovery();

  @Mock
  private Emitter emitter;

  @Captor
  private ArgumentCaptor<MagpieEnvelope> envelopeCapture;

  @BeforeAll
  public static void setup() {
    updateStackWithResources(CF_CLOUDWATCH_TEMPLATE_PATH);
  }

  @Test
  public void testCloudWatchDiscovery() {
    createAlarm();

    cloudWatchDiscovery.discover(
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
  }

  private void createAlarm() {
    CloudWatchClient client = ClientCreators.localClientCreator(BASE_REGION).apply(CloudWatchClient.builder()).build();

    Dimension dimension = Dimension.builder()
      .name("InstanceId")
      .value("instanceid").build();

    String testalarm = "testalarm";
    PutMetricAlarmRequest request = PutMetricAlarmRequest.builder()
      .alarmName(testalarm)
      .comparisonOperator(
        ComparisonOperator.GREATER_THAN_THRESHOLD)
      .evaluationPeriods(1)
      .metricName("CPUUtilization")
      .namespace("AWS/EC2")
      .period(60)
      .statistic(Statistic.AVERAGE)
      .threshold(70.0)
      .actionsEnabled(false)
      .alarmDescription(
        "Alarm when server CPU utilization exceeds 70%")
      .unit(StandardUnit.SECONDS)
      .dimensions(dimension)
      .build();

    client.putMetricAlarm(request);
  }


}
