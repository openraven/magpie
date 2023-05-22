package io.openraven.magpie.plugins.aws.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import java.net.URI;
import java.util.UUID;

public class ClientCreators {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientCreators.class);

  //This client does not need to be recreated on every request.
  public static final StsClient localStsClient = StsClient.create();

  public static MagpieAWSClientCreator assumeRoleCreator(final Region region, final String roleArn, String externalId, AWSDiscoveryConfig.ProxyRoleConfig proxyRoleConfig) {
    return new MagpieAWSClientCreator(){
      @Override
      public <BuilderT extends AwsClientBuilder<BuilderT, ClientT>, ClientT> BuilderT apply(AwsClientBuilder<BuilderT, ClientT> builder) {
        final var magpieAwsEndpoint = System.getProperty("MAGPIE_AWS_ENDPOINT");
        if (magpieAwsEndpoint != null) {
          builder.endpointOverride(URI.create(magpieAwsEndpoint));
        }

        var stsClient = localStsClient;

        if(proxyRoleConfig != null) {

          final var arn = proxyRoleConfig.getArn();
          final var proxyRoleConfigExternalId = proxyRoleConfig.getExternalId();

          LOGGER.debug("Using proxyRoleConfig with arn={}, externalId={} connecting to roleArn={}, externalId={}", arn, proxyRoleConfigExternalId, roleArn, externalId);
          final AssumeRoleRequest.Builder assumeRoleRequestBuilder = AssumeRoleRequest.builder()
            .roleArn(arn)
            .externalId(proxyRoleConfigExternalId)
            .roleSessionName(UUID.randomUUID().toString());

          final var provider = StsAssumeRoleCredentialsProvider.builder()
            .stsClient(localStsClient)
            .refreshRequest(
              assumeRoleRequestBuilder
                .build()
            ).build();

          stsClient = StsClient.builder().credentialsProvider(provider).build();
        }

          final AssumeRoleRequest.Builder assumeRoleRequestBuilder = AssumeRoleRequest.builder()
                  .roleArn(roleArn)
                  .roleSessionName(UUID.randomUUID().toString())
                  .externalId(externalId);

          final var provider = StsAssumeRoleCredentialsProvider.builder()
          .stsClient(stsClient)
          .refreshRequest(
            assumeRoleRequestBuilder
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
