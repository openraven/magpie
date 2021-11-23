package io.openraven.magpie.plugins.aws.discovery;

import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import java.net.URI;
import java.util.UUID;

public class ClientCreators {

  public static MagpieAWSClientCreator assumeRoleCreator(final Region region, final String roleArn) {
    return new MagpieAWSClientCreator(){
      @Override
      public <BuilderT extends AwsClientBuilder<BuilderT, ClientT>, ClientT> BuilderT apply(AwsClientBuilder<BuilderT, ClientT> builder) {
        final var magpieAwsEndpoint = System.getProperty("MAGPIE_AWS_ENDPOINT");
        if (magpieAwsEndpoint != null) {
          builder.endpointOverride(URI.create(magpieAwsEndpoint));
        }
        final var provider = StsAssumeRoleCredentialsProvider.builder()
          .stsClient(StsClient.create())
          .refreshRequest(
            AssumeRoleRequest.builder()
              .roleArn(roleArn)
              .roleSessionName(UUID.randomUUID().toString())
              .build()
          ).build();
        return builder.credentialsProvider(provider).region(region);
      }
    };
  }

  public static MagpieAWSClientCreator localClientCreator(final Region region) {
    return new MagpieAWSClientCreator(){
      @Override
      public <BuilderT extends AwsClientBuilder<BuilderT, ClientT>, ClientT> BuilderT apply(AwsClientBuilder<BuilderT, ClientT> builder) {
        final var magpieAwsEndpoint = System.getProperty("MAGPIE_AWS_ENDPOINT");
        if (magpieAwsEndpoint != null) {
          builder.endpointOverride(URI.create(magpieAwsEndpoint));
        }
        return builder.region(region);
      }
    };
  }
}
