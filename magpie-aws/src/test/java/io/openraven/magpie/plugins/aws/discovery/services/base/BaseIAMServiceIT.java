package io.openraven.magpie.plugins.aws.discovery.services.base;

import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.PolicyScopeType;

public abstract class BaseIAMServiceIT extends BaseAWSServiceIT {

  protected static final IamClient IAMCLIENT = AWSUtils.configure(IamClient.builder(), BASE_REGION);

  protected static void removePolicies() {
    IAMCLIENT.listPolicies(req -> req.scope(PolicyScopeType.LOCAL))
      .policies().forEach(policy -> IAMCLIENT.deletePolicy(req -> req.policyArn(policy.arn())));
  }

}
