package io.openraven.magpie.plugins.aws.discovery;

import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.regions.Region;

import java.net.URI;

public class ClientCreators {

  public static MagpieAWSClientCreator localClientCreator(Region region) {
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
