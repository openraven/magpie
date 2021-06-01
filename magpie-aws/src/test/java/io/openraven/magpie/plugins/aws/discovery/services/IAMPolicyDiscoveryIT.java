package io.openraven.magpie.plugins.aws.discovery.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.atLeast;

@ExtendWith(MockitoExtension.class)
public class IAMPolicyDiscoveryIT extends BaseAWSServiceIT {

  private final String POLICY_DYNAMODB_PATH = "/document/policy-dynamodb-access.json";
  private final String POLICY_DYNAMODB_UPDATED_PATH = "/document/policy-dynamodb-updated-access.json";
  private final String POLICY_NAME = "PolicyDataAccess";
  private final IAMDiscovery iamDiscovery = new IAMDiscovery();

  @Mock
  private Emitter emitter;

  @Captor
  private ArgumentCaptor<MagpieEnvelope> envelopeCapture;

  @Test
  public void testPolicyDiscovery() {

    var iamClient = IamClient.builder()
      .endpointOverride(URI.create(System.getProperty("MAGPIE_AWS_ENDPOINT")))
      .region(BASE_REGION)
      .build();

    // then
    var policyArn = createPolicy(iamClient);
    createPolicyVersion(iamClient, policyArn);
    // when
    iamDiscovery.discoverPolicies(
      iamClient,
      MAPPER,
      SESSION,
      BASE_REGION,
      emitter,
      ACCOUNT
    );

    // then
    Mockito.verify(emitter, atLeast(1)).emit(envelopeCapture.capture());
    var envelope = envelopeCapture.getValue();

    assertPolicy(envelope);
    assertConfiguration(envelope);
    assertPolicyDocument(envelope);
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
    assertEquals(String.format("arn:aws:iam::000000000000:policy/%s", POLICY_NAME), contents.get("arn").asText());
    assertEquals(POLICY_NAME, contents.get("resourceName").asText());
    assertNotNull(contents.get("resourceId").asText());
    assertEquals("AWS::IAM::Policy", contents.get("resourceType").asText());
    assertEquals(ACCOUNT, contents.get("awsAccountId").asText());
    assertEquals(BASE_REGION.toString(), contents.get("awsRegion").asText());
  }

  private String createPolicy(IamClient iamClient) {
    return iamClient.createPolicy(policyRequest -> policyRequest
        .policyName(POLICY_NAME)
        .description("Tax data access")
        .policyDocument(getResourceAsString(POLICY_DYNAMODB_PATH)))
      .policy()
      .arn();
  }

  private void createPolicyVersion(IamClient iamClient, String arn) {
    iamClient.createPolicyVersion(policyVersion -> policyVersion
      .policyArn(arn)
      .policyDocument(getResourceAsString(POLICY_DYNAMODB_UPDATED_PATH))
      .setAsDefault(true));
  }

}
