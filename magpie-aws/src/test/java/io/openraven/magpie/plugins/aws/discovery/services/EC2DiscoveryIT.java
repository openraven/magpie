package io.openraven.magpie.plugins.aws.discovery.services;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.plugins.aws.discovery.BackupUtils;
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
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.BackupJob;
import software.amazon.awssdk.services.backup.model.ListBackupJobsRequest;
import software.amazon.awssdk.services.backup.model.ListBackupJobsResponse;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EC2DiscoveryIT extends BaseAWSServiceIT {

  private static final String CF_EC2_TEMPLATE_PATH = "/template/ec2-template.yml";
  private EC2Discovery ec2Discovery = new EC2Discovery();

  @Mock
  private Emitter emitter;

  @Mock
  private BackupClient backupClient;

  @Captor
  private ArgumentCaptor<MagpieEnvelope> envelopeCapture;

  @BeforeAll
  public static void setup() {
    updateStackWithResources(CF_EC2_TEMPLATE_PATH);
  }

  @Test
  public void testEC2Discovery() {
    // given
    BackupUtils.init(BASE_REGION, backupClient);
    when(backupClient.listBackupJobs(any(ListBackupJobsRequest.class)))
      .thenReturn(ListBackupJobsResponse.builder().backupJobs(Collections.emptyList()).build());

    // when
    ec2Discovery.discover(
      MAPPER,
      SESSION,
      BASE_REGION,
      emitter,
      LOGGER,
      ACCOUNT
    );

    // then
    Mockito.verify(emitter, atLeast(1)).emit(envelopeCapture.capture());

    var resources = envelopeCapture.getAllValues().stream()
      .map(MagpieEnvelope::getContents)
      .collect(Collectors.groupingBy(val -> val.get("resourceType").asText()));

    assertInstance(resources.get("AWS::EC2::Instance"));
    assertEIP(resources.get("AWS::EC2::EIP"));
    assertSecurityGroup(resources.get("AWS::EC2::SecurityGroup"));
    assertVolume(resources.get("AWS::EC2::Volume"));
    assertSnapshot(resources.get("AWS::EC2::Snapshot"));
  }

  private void assertSnapshot(List<ObjectNode> data) {
    assertTrue(30 < data.size()); // Avoid flaky tests with backups generation
    var contents = data.get(0);

    assertNotNull(contents.get("documentId"));
    assertTrue(contents.get("assetId").asText().startsWith("arn:aws:ec2:us-west-1:account:snapshot/snap-"));
    assertTrue(contents.get("resourceName").asText().startsWith("snap-"));
    assertEquals(contents.get("resourceName").asText(), contents.get("resourceId").asText());
    assertEquals("AWS::EC2::Snapshot", contents.get("resourceType").asText());
    assertEquals(ACCOUNT, contents.get("accountId").asText());
    assertEquals(BASE_REGION.toString(), contents.get("region").asText());

    var configuration = contents.get("configuration");
    assertTrue(configuration.get("description").asText().startsWith("Auto-created snapshot for AMI ami-"));
    assertEquals("completed", configuration.get("state").asText());
    assertTrue(configuration.get("snapshotId").asText().startsWith("snap-"));
  }

  private void assertVolume(List<ObjectNode> data) {
    assertEquals(1, data.size());
    var contents = data.get(0);

    assertNotNull(contents.get("documentId"));
    assertTrue(contents.get("assetId").asText().startsWith("arn:aws:ec2:us-west-1:account:volume/vol-"));
    assertTrue(contents.get("resourceName").asText().startsWith("vol-"));
    assertEquals(contents.get("resourceName").asText(), contents.get("resourceId").asText());
    assertEquals("AWS::EC2::Volume", contents.get("resourceType").asText());
    assertEquals(ACCOUNT, contents.get("accountId").asText());
    assertEquals(BASE_REGION.toString(), contents.get("region").asText());

    var configuration = contents.get("configuration");
    assertTrue(configuration.get("volumeId").asText().startsWith("vol-"));
    assertEquals("standard", configuration.get("volumeType").asText());

    var attachments = configuration.get("attachments");
    assertEquals(1, attachments.size());
    var attachment = attachments.get(0);
    assertNotNull(attachment.get("attachTime").asText());
    assertEquals("/dev/sda1", attachment.get("device").asText());
    assertTrue(attachment.get("instanceId").asText().startsWith("i-"));
    assertTrue(attachment.get("volumeId").asText().startsWith("vol-"));
    assertEquals("attached", attachment.get("state").asText());

  }

  private void assertSecurityGroup(List<ObjectNode> data) {
    assertEquals(2, data.size()); // There default and default VPC
    var contents = data.get(0);

    assertNotNull(contents.get("documentId"));
    assertTrue(contents.get("assetId").asText().startsWith("arn:aws:ec2:us-west-1:account:security-group/sg-"));
    assertEquals("default", contents.get("resourceName").asText());
    assertTrue( contents.get("resourceId").asText().startsWith("sg-"));
    assertEquals("AWS::EC2::SecurityGroup", contents.get("resourceType").asText());
    assertEquals(ACCOUNT, contents.get("accountId").asText());
    assertEquals(BASE_REGION.toString(), contents.get("region").asText());

    var configuration = contents.get("configuration");
    assertEquals("default group", configuration.get("description").asText());
    assertEquals("default", configuration.get("groupName").asText());
    assertEquals("000000000000", configuration.get("ownerId").asText());
  }

  private void assertEIP(List<ObjectNode> data) {
    assertEquals(1, data.size());
    var contents = data.get(0);

    assertNotNull(contents.get("documentId"));
    assertEquals("arn:aws:ec2:us-west-1:account:eip-allocation/null", contents.get("assetId").asText());
    // IP Address
    assertEquals(contents.get("resourceName").asText(), contents.get("configuration").get("publicIp").asText());
    assertEquals("AWS::EC2::EIP", contents.get("resourceType").asText());
    assertEquals(ACCOUNT, contents.get("accountId").asText());
    assertEquals(BASE_REGION.toString(), contents.get("region").asText());

    var configuration = contents.get("configuration");
    assertTrue(configuration.get("instanceId").asText().startsWith("i-"));
    assertTrue(configuration.get("publicIp").asText().startsWith("127"));
    assertTrue(configuration.get("networkInterfaceId").asText().startsWith("eni-"));
    assertEquals("standard", configuration.get("domain").asText());
  }

  private void assertInstance(List<ObjectNode> data) {
    assertEquals(1, data.size());
    var contents = data.get(0);

    assertNotNull(contents.get("documentId"));
    assertTrue(contents.get("assetId").asText().startsWith("arn:aws:ec2:us-west-1:000000000000:instance"));
    assertFalse(contents.get("resourceName").asText().isEmpty());
    assertEquals("AWS::EC2::Instance", contents.get("resourceType").asText());
    assertEquals(ACCOUNT, contents.get("accountId").asText());
    assertEquals(BASE_REGION.toString(), contents.get("region").asText());

    var configuration = contents.get("configuration");
    assertTrue(configuration.get("imageId").asText().startsWith("ami-"));
    assertTrue(configuration.get("instanceId").asText().startsWith("i-"));
    assertEquals("m1.small", configuration.get("instanceType").asText());
    assertEquals("testkey", configuration.get("keyName").asText());
    assertNotNull(configuration.get("launchTime").asText());
    assertTrue(configuration.get("placement").get("availabilityZone").asText().startsWith(BASE_REGION.toString()));

    assertTrue(configuration.get("privateDnsName").asText().endsWith("us-west-1.compute.internal"));
    assertFalse(configuration.get("privateIpAddress").asText().isEmpty());
    assertTrue(configuration.get("publicDnsName").asText().endsWith("us-west-1.compute.amazonaws.com"));
    assertFalse(configuration.get("publicIpAddress").asText().isEmpty());

    var blockDeviceMappings = configuration.get("blockDeviceMappings");
    assertEquals(1, blockDeviceMappings.size());
    var device = blockDeviceMappings.get(0);
    assertEquals("/dev/sda1", device.get("deviceName").asText());
    assertEquals("in-use", device.get("ebs").get("status").asText());
    assertTrue(device.get("ebs").get("volumeId").asText().startsWith("vol-"));
    assertEquals("/dev/sda1" ,configuration.get("rootDeviceName").asText());
    assertEquals("ebs" ,configuration.get("rootDeviceType").asText());
    assertNotNull(configuration.get("publicIp").asText());
  }
}
