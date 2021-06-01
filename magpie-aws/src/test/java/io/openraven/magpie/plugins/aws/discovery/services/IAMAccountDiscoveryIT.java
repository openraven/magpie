package io.openraven.magpie.plugins.aws.discovery.services;


import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.api.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iam.IamClient;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.atLeast;


@ExtendWith(MockitoExtension.class)
public class IAMAccountDiscoveryIT extends BaseAWSServiceIT {

  private final String allias = "AccountTestAllias";
  private final String mfaDeviceName = "testdevice";
  private final IAMDiscovery iamDiscovery = new IAMDiscovery();

  @Mock
  private Emitter emitter;

  @Captor
  private ArgumentCaptor<MagpieEnvelope> envelopeCapture;

  @Test
  public void testAccountDiscovery() {
    var iamClient = IamClient.builder()
      .endpointOverride(URI.create(System.getProperty("MAGPIE_AWS_ENDPOINT")))
      .region(BASE_REGION)
      .build();

    // given
    createAccountAlliases(iamClient, allias);
    createPasswordPolicy(iamClient);
    //createMFADevice(iamClient);

    // when
    iamDiscovery.discoverAccounts(
      iamClient,
      MAPPER,
      SESSION,
      BASE_REGION,
      emitter,
      ACCOUNT
    );

    // then
    Mockito.verify(emitter, atLeast(1)).emit(envelopeCapture.capture());
    assertEquals(1, envelopeCapture.getAllValues().size());
    var envelope = envelopeCapture.getValue();

    assertAccount(envelope);
    assertConfiguration(envelope);
  }

  private void assertConfiguration(MagpieEnvelope envelope) {
    var supplementaryConfiguration = envelope.getContents().get("supplementaryConfiguration");
    var passwordPolicy = supplementaryConfiguration.get("PasswordPolicy");
    assertEquals("{\"minimumPasswordLength\":8,\"requireSymbols\":true,\"requireNumbers\":true,\"requireUppercaseCharacters\":true,\"requireLowercaseCharacters\":true,\"allowUsersToChangePassword\":true,\"expirePasswords\":true,\"maxPasswordAge\":200,\"passwordReusePrevention\":1,\"hardExpiry\":true}",
      passwordPolicy.toString());
    var summaryMap = supplementaryConfiguration.get("summaryMap");
    assertEquals("{\"GroupPolicySizeQuota\":5120,\"InstanceProfilesQuota\":1000,\"Policies\":0,\"GroupsPerUserQuota\":10,\"InstanceProfiles\":0,\"AttachedPoliciesPerUserQuota\":10,\"Users\":0,\"PoliciesQuota\":1500,\"Providers\":0,\"AccountMFAEnabled\":0,\"AccessKeysPerUserQuota\":2,\"AssumeRolePolicySizeQuota\":2048,\"PolicyVersionsInUseQuota\":10000,\"GlobalEndpointTokenVersion\":1,\"VersionsPerPolicyQuota\":5,\"AttachedPoliciesPerGroupQuota\":10,\"PolicySizeQuota\":6144,\"Groups\":0,\"AccountSigningCertificatesPresent\":0,\"UsersQuota\":5000,\"ServerCertificatesQuota\":20,\"MFADevices\":0,\"UserPolicySizeQuota\":2048,\"PolicyVersionsInUse\":0,\"ServerCertificates\":0,\"Roles\":0,\"RolesQuota\":1000,\"SigningCertificatesPerUserQuota\":2,\"MFADevicesInUse\":0,\"RolePolicySizeQuota\":10240,\"AttachedPoliciesPerRoleQuota\":10,\"AccountAccessKeysPresent\":0,\"GroupsQuota\":300}",
      summaryMap.toString());
  }

  private void assertAccount(MagpieEnvelope envelope) {
    var contents = envelopeCapture.getValue().getContents();
    assertNotNull(contents.get("documentId").asText());
    assertEquals("AWS::IAM::Account", contents.get("arn").asText());
    assertEquals(allias, contents.get("resourceName").asText());
    assertEquals("AWS::IAM::Account", contents.get("resourceType").asText());
    assertEquals(ACCOUNT, contents.get("awsAccountId").asText());
    assertEquals(BASE_REGION.toString(), contents.get("awsRegion").asText());
  }

  private void createMFADevice(IamClient iamClient) {
    iamClient.createVirtualMFADevice(req -> req
      .virtualMFADeviceName(mfaDeviceName));
  }

  private void createPasswordPolicy(IamClient iamClient) {
    iamClient.updateAccountPasswordPolicy(req -> req
      .maxPasswordAge(200)
      .allowUsersToChangePassword(true)
      .minimumPasswordLength(8)
      .hardExpiry(true)
      .passwordReusePrevention(1)
      .requireLowercaseCharacters(true)
      .requireNumbers(true)
      .requireUppercaseCharacters(true)
      .requireSymbols(true)
    );
  }

  private void createAccountAlliases(IamClient iamClient, String allias) {
    iamClient.createAccountAlias(req -> req.accountAlias(allias));
  }



}
