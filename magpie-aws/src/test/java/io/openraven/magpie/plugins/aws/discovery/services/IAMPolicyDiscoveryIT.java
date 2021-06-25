package io.openraven.magpie.plugins.aws.discovery.services;

import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.plugins.aws.discovery.services.base.BaseIAMServiceIT;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class IAMPolicyDiscoveryIT extends BaseIAMServiceIT {

  private final String POLICY_DYNAMODB_PATH = "/document/policy-dynamodb-access.json";
  private final String POLICY_DYNAMODB_UPDATED_PATH = "/document/policy-dynamodb-updated-access.json";
  private final String POLICY_NAME = "PolicyDataAccess";
  private final IAMDiscovery iamDiscovery = new IAMDiscovery();

  @Mock
  private Emitter emitter;

  @Captor
  private ArgumentCaptor<MagpieEnvelope> envelopeCapture;

  @AfterAll
  public static void cleanup() {
    removePolicies();
  }

  @Test
  public void testPolicyDiscovery() {
    // then
    var policyArn = createPolicy();
    createPolicyVersion(policyArn);
    // when
    iamDiscovery.discoverPolicies(
      IAMCLIENT,
      MAPPER,
      SESSION,
      BASE_REGION,
      emitter,
      ACCOUNT
    );
    // then

    Mockito.verify(emitter, Mockito.atLeastOnce()).emit(envelopeCapture.capture());

    List<MagpieEnvelope> envelopes = envelopeCapture.getAllValues();
    var filteredPolicies = envelopes.stream().filter(envelope ->
      POLICY_NAME.equals(envelope.getContents().get("resourceName").asText())
    ).collect(Collectors.toList());

    assertEquals(1, filteredPolicies.size());

    assertPolicy(filteredPolicies.get(0));
    assertConfiguration(filteredPolicies.get(0));
    assertPolicyDocument(filteredPolicies.get(0));
  }

  private void assertPolicyDocument(MagpieEnvelope envelope) {
    var supplementaryConfiguration = envelope.getContents().get("supplementaryConfiguration");
    var attachedPolicies = supplementaryConfiguration.get("attachedPolicies");
    var policyDocument = attachedPolicies.get("policyDocument").asText();
    assertEquals(getResourceAsString(POLICY_DYNAMODB_UPDATED_PATH), policyDocument);
  }

  private void assertConfiguration(MagpieEnvelope envelope) {
    var configuration = envelope.getContents().get("configuration");
    assertEquals(POLICY_NAME, configuration.get("policyName").asText());
    assertEquals("v2", configuration.get("defaultVersionId").asText()); // Second Version should be default
  }

  private void assertPolicy(MagpieEnvelope envelope) {
    var contents = envelope.getContents();
    assertNotNull(contents.get("documentId"));
    assertEquals(String.format("arn:aws:iam::000000000000:policy/%s", POLICY_NAME), contents.get("assetId").asText());
    assertEquals(POLICY_NAME, contents.get("resourceName").asText());
    assertNotNull(contents.get("resourceId").asText());
    assertEquals("AWS::IAM::Policy", contents.get("resourceType").asText());
    assertEquals(ACCOUNT, contents.get("accountId").asText());
    assertEquals(BASE_REGION.toString(), contents.get("region").asText());
  }

  private String createPolicy() {
    return IAMCLIENT.createPolicy(policyRequest -> policyRequest
      .policyName(POLICY_NAME)
      .description("Tax data access")
      .policyDocument(getResourceAsString(POLICY_DYNAMODB_PATH)))
      .policy()
      .arn();
  }

  private void createPolicyVersion(String arn) {
    IAMCLIENT.createPolicyVersion(policyVersion -> policyVersion
      .policyArn(arn)
      .policyDocument(getResourceAsString(POLICY_DYNAMODB_UPDATED_PATH))
      .setAsDefault(true));
  }

}
