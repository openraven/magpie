package io.openraven.magpie.plugins.aws.discovery.services;


import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.plugins.aws.discovery.services.base.BaseIAMServiceIT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.atLeast;

@ExtendWith(MockitoExtension.class)
public class IAMAccountDiscoveryIT extends BaseIAMServiceIT {

  private final String alias = "AccountTestAllias";
  private final String mfaDeviceName = "testdevice";
  private final IAMDiscovery iamDiscovery = new IAMDiscovery();

  @Mock
  private Emitter emitter;

  @Captor
  private ArgumentCaptor<MagpieEnvelope> envelopeCapture;

  @Test
  public void testAccountDiscovery() {
    // given
    createAccountAliases(alias);
    createPasswordPolicy();

    // when
    iamDiscovery.discoverAccounts(
      IAMCLIENT,
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
    var configuration = envelope.getContents().get("configuration");
    assertNotNull(configuration);

    var supplementaryConfiguration = envelope.getContents().get("supplementaryConfiguration");
    var passwordPolicy = supplementaryConfiguration.get("PasswordPolicy");
    assertEquals("{\"minimumPasswordLength\":8,\"requireSymbols\":true,\"requireNumbers\":true,\"requireUppercaseCharacters\":true,\"requireLowercaseCharacters\":true,\"allowUsersToChangePassword\":true,\"expirePasswords\":true,\"maxPasswordAge\":200,\"passwordReusePrevention\":1,\"hardExpiry\":true}",
      passwordPolicy.toString());
  }

  private void assertAccount(MagpieEnvelope envelope) {
    var contents = envelopeCapture.getValue().getContents();
    assertNotNull(contents.get("documentId").asText());
    assertEquals("arn:aws:organizations::account", contents.get("arn").asText());
    assertEquals(alias, contents.get("resourceName").asText());
    assertEquals("AWS::Account", contents.get("resourceType").asText());
    assertEquals(ACCOUNT, contents.get("awsAccountId").asText());
  }

  private void createPasswordPolicy() {
    IAMCLIENT.updateAccountPasswordPolicy(req -> req
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

  private void createAccountAliases(String alias) {
    IAMCLIENT.createAccountAlias(req -> req.accountAlias(alias));
  }


}
