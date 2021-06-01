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
import software.amazon.awssdk.services.iam.model.CreatePolicyResponse;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.atLeast;

/**
 * Covered IAM Groups. ResourceGroups not covered
 */
@ExtendWith(MockitoExtension.class)
public class IAMGroupDiscoveryIT extends BaseAWSServiceIT {

  private final String POLICY_DOCUMENT_PATH = "/document/policy-dynamodb-access.json";
  private final String INLINE_POLICY_DOCUMENT_PATH = "/document/policy-dynamodb-inline.json";
  private final String GROUP_NAME = "Accountants";
  private final String MANAGED_POLICY_NAME = "managedDataAccess";
  private final String INLINE_POLICY_NAME = "inlineDataAccess";

  private final IAMDiscovery iamDiscovery = new IAMDiscovery();

  @Mock
  private Emitter emitter;

  @Captor
  private ArgumentCaptor<MagpieEnvelope> envelopeCapture;

  @Test
  public void testGroupDiscovery() {
    IamClient iamClient = IamClient.builder()
      .endpointOverride(URI.create(System.getProperty("MAGPIE_AWS_ENDPOINT")))
      .region(BASE_REGION)
      .build();

    // Group creation failing with CF template. Fallback to SDK manual approach
    createGroupWithInlinePolicy(iamClient);
    String attachedPolicyArn = createPolicyAndAttach(iamClient);

    // when
    iamDiscovery.discoverGroups(
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
    MagpieEnvelope envelope = envelopeCapture.getValue();

    assertGroup(envelope);
    assertInlinePolicies(envelope);
    assertAttachedPolicies(envelope);

    //clenup
    iamClient.deletePolicy(req -> req.policyArn(attachedPolicyArn));
    iamClient.deleteGroup(req -> req.groupName(GROUP_NAME));
  }

  private void assertGroup(MagpieEnvelope envelope) {
    var contents = envelope.getContents();
    assertNotNull(contents.get("documentId"));
    assertEquals(String.format("arn:aws:iam::000000000000:group/%s", GROUP_NAME), contents.get("arn").asText());
    assertEquals(GROUP_NAME, contents.get("resourceName").asText());
    assertNotNull(contents.get("resourceId").asText());
    assertEquals("AWS::IAM::Group", contents.get("resourceType").asText());
    assertEquals(ACCOUNT, contents.get("awsAccountId").asText());
    assertEquals(BASE_REGION.toString(), contents.get("awsRegion").asText());
  }

  private void assertInlinePolicies(MagpieEnvelope envelope) {
    var inlinePolicies = envelope.getContents().get("supplementaryConfiguration").get("inlinePolicies");
    assertEquals(1, inlinePolicies.size());

    var inlinePolicy = inlinePolicies.get(0);
    assertEquals("inlineDataAccess", inlinePolicy.get("name").asText());
    assertEquals(getResourceAsString(INLINE_POLICY_DOCUMENT_PATH), inlinePolicy.get("policyDocument").asText());
  }

  private void assertAttachedPolicies(MagpieEnvelope envelope) {
    var attachedPolicies = envelope.getContents().get("supplementaryConfiguration").get("attachedPolicies");
    assertEquals(1, attachedPolicies.size());

    var policy = attachedPolicies.get(0);
    assertEquals("managedDataAccess", policy.get("name").asText());
    assertEquals(String.format("arn:aws:iam::000000000000:policy/%s", MANAGED_POLICY_NAME), policy.get("arn").asText());

  }

  private void createGroupWithInlinePolicy(IamClient iamClient) {
    iamClient.createGroup(request -> {
      request.groupName(GROUP_NAME);
    });
    iamClient.putGroupPolicy(p -> p
      .groupName(GROUP_NAME)
      .policyName(INLINE_POLICY_NAME)
      .policyDocument(getResourceAsString(INLINE_POLICY_DOCUMENT_PATH))
    );
  }

  private String createPolicyAndAttach(IamClient iamClient) {
    CreatePolicyResponse policyResp = iamClient.createPolicy(policyRequest -> policyRequest
      .policyName(MANAGED_POLICY_NAME)
      .description("Tax data access")
      .policyDocument(getResourceAsString(POLICY_DOCUMENT_PATH))
    );

    iamClient.attachGroupPolicy(attach -> attach.groupName(GROUP_NAME).policyArn(policyResp.policy().arn()));
    return policyResp.policy().arn();
  }
}
