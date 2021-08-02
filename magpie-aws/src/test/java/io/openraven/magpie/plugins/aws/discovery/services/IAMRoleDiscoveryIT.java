package io.openraven.magpie.plugins.aws.discovery.services;


import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.plugins.aws.discovery.services.base.BaseIAMServiceIT;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.atLeast;

@ExtendWith(MockitoExtension.class)
public class IAMRoleDiscoveryIT extends BaseIAMServiceIT {
  private static final String CF_IAM_ROLE_TEMPLATE_PATH = "/template/iam-role-template.yml";
  private static final String ROLE_NAME = "RootRole";

  private final IAMDiscovery iamDiscovery = new IAMDiscovery();

  @Mock
  private Emitter emitter;

  @Captor
  private ArgumentCaptor<MagpieEnvelope> envelopeCapture;

  @BeforeAll
  public static void setup() {
    updateStackWithResources(CF_IAM_ROLE_TEMPLATE_PATH);
  }

  @AfterAll
  public static void cleanup() {
    removePolicies();
  }

  @Test
  public void testRoleDiscovery() {
    // when
    iamDiscovery.discoverRoles(
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
    MagpieEnvelope envelope = envelopeCapture.getValue();
    ObjectNode contents = envelope.getContents();

    assertRole(envelope);
    assertConfiguration(envelope);
    assertInlinePolicy(envelope);
  }

  private void assertRole(MagpieEnvelope envelope) {
    var contents = envelope.getContents();
    assertNotNull(contents.get("documentId"));
    assertTrue(contents.get("assetId").asText().contains(ROLE_NAME));
    assertTrue(contents.get("resourceName").asText().contains(ROLE_NAME));
    assertNotNull(contents.get("resourceId").asText());
    assertEquals("AWS::IAM::Role", contents.get("resourceType").asText());
    assertEquals(ACCOUNT, contents.get("accountId").asText());
    assertEquals(BASE_REGION.toString(), contents.get("region").asText());
  }

  private void assertConfiguration(MagpieEnvelope envelope) {
    var configuration = envelope.getContents().get("configuration");
    assertEquals("/", configuration.get("path").asText());
    assertNotNull(configuration.get("roleId"));
    assertTrue(configuration.get("arn").asText().contains(ROLE_NAME));
    assertNotNull(configuration.get("createDate"));
    assertNotNull(configuration.get("assumeRolePolicyDocument"));
  }

  private void assertInlinePolicy(MagpieEnvelope envelope) {
    var inlinePolicies = envelope.getContents().get("supplementaryConfiguration").get("inlinePolicies");
    assertEquals(2, inlinePolicies.size());

    var inlinePolicy = inlinePolicies.get(0);
    assertEquals("inlinePolicy", inlinePolicy.get("name").asText());
    assertEquals("{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"*\",\"Resource\":\"*\"}]}",
      inlinePolicy.get("policyDocument").toString()
    );
  }
}

