package io.openraven.magpie.plugins.aws.discovery.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieAwsResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.aws.organisation.ServiceControlPolicy;
import io.openraven.magpie.data.aws.shared.PayloadUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.organizations.OrganizationsClient;
import software.amazon.awssdk.services.organizations.model.DescribePolicyRequest;
import software.amazon.awssdk.services.organizations.model.DescribePolicyResponse;
import software.amazon.awssdk.services.organizations.model.ListPoliciesRequest;
import software.amazon.awssdk.services.organizations.model.ListPoliciesResponse;
import software.amazon.awssdk.services.organizations.model.Policy;
import software.amazon.awssdk.services.organizations.paginators.ListPoliciesIterable;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class OrganizationDiscovery implements AWSDiscovery {

  private static final String SERVICE = "organization";
  private static final String SERVICE_CONTROL_POLICY = "SERVICE_CONTROL_POLICY";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return OrganizationsClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {
    try (final var client = clientCreator.apply(OrganizationsClient.builder()).build()) {
      discoverPolicies(client, mapper, session, region, emitter, account);
    }
  }

  protected void discoverPolicies(OrganizationsClient client, ObjectMapper mapper, Session session, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = ServiceControlPolicy.RESOURCE_TYPE;

    try {
      ListPoliciesIterable listPoliciesIterable = client.listPoliciesPaginator(ListPoliciesRequest.builder()
        .filter(SERVICE_CONTROL_POLICY)
        .build());

      for (ListPoliciesResponse listPoliciesResponse : listPoliciesIterable) {

        listPoliciesResponse.policies().forEach(policySummary -> {

            DescribePolicyRequest describePolicyRequest = DescribePolicyRequest.builder()
              .policyId(policySummary.id())
              .build();

            DescribePolicyResponse describePolicyResponse = client.describePolicy(describePolicyRequest);

            Policy policy = describePolicyResponse.policy();

            var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, policySummary.arn())
              .withResourceName(policySummary.name())
              .withResourceId(policySummary.id())
              .withResourceType(RESOURCE_TYPE)
              .withConfiguration(PayloadUtils.update(policySummary))
              .withSupplementaryConfiguration(parsePolicyDocument(mapper, policy.content()))
              .withAccountId(account)
              .withAwsRegion(region.id())
              .build();

           emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":policy"), data.toJsonNode()));

          });
      }

    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private JsonNode parsePolicyDocument(ObjectMapper mapper, String policyDocument) {
    try {
      return mapper.readTree(URLDecoder.decode(policyDocument, StandardCharsets.UTF_8));
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Unable to parse inline policy document: " + policyDocument, e);
    }
  }

}
