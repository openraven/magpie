package io.openraven.magpie.plugins.aws.discovery;

import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;

public interface MagpieAWSClientCreator {
  <BuilderT extends AwsClientBuilder<BuilderT, ClientT>, ClientT> BuilderT apply(AwsClientBuilder<BuilderT,ClientT> builder);
}
