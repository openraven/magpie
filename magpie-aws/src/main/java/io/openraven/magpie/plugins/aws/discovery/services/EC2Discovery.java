/*
 * Copyright 2021 Open Raven Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openraven.magpie.plugins.aws.discovery.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.MagpieAwsResource;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.data.aws.ec2.EC2SecurityGroup;
import io.openraven.magpie.data.aws.ec2.Ec2ElasticIpAddress;
import io.openraven.magpie.data.aws.ec2.Ec2Instance;
import io.openraven.magpie.data.aws.ec2.Ec2NetworkAcl;
import io.openraven.magpie.data.aws.ec2.Ec2TransitGateway;
import io.openraven.magpie.plugins.aws.discovery.AWSDiscoveryPlugin;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.MagpieAWSClientCreator;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import io.sentry.Sentry;
import io.sentry.event.Event;
import io.sentry.event.EventBuilder;
import io.sentry.event.interfaces.ExceptionInterface;
import kong.unirest.HttpResponse;
import kong.unirest.HttpStatus;
import kong.unirest.Unirest;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;
import static java.lang.String.format;

public class EC2Discovery implements AWSDiscovery {

  private static final String SERVICE = "ec2";
  private static final Pattern CIDR_REGEX = Pattern.compile("^((?:[0-9]{1,3}\\.){3}[0-9]{1,3})/([0-9]|[1-2][0-9]|3[0-2])?$");
  private static final String SINGLE_HOST_NETMASK = "255.255.255.255";

  private final Map<String, JsonNode> whoisCache = new HashMap<>();

  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account, MagpieAWSClientCreator clientCreator) {

    try (final var client = clientCreator.apply(Ec2Client.builder()).build()) {
      discoverEc2Instances(mapper, session, client, region, emitter, account, clientCreator, logger);
      discoverEIPs(mapper, session, client, region, emitter, account);
      discoverSecurityGroups(mapper, session, client, region, emitter, account, logger);
      discoverNetworkAcls(mapper, session, client, region, emitter, account);
      discoverTransitGateway(mapper, session, client, region, emitter, account);
    }
  }

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return Ec2Client.serviceMetadata().regions();
  }

  private void discoverEc2Instances(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account, MagpieAWSClientCreator clientCreator, Logger logger) {

    final String RESOURCE_TYPE = Ec2Instance.RESOURCE_TYPE;
    try {
      client.describeInstancesPaginator()
        .forEach(describeInstancesResponse -> describeInstancesResponse.reservations()
          .forEach(reservation -> reservation.instances().forEach(instance -> {
            String arn = format("arn:aws:ec2:%s:%s:instance/%s", region, reservation.ownerId(), instance.instanceId());
            var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, arn)
              .withResourceName(instance.instanceId())
              .withResourceId(instance.instanceId())
              .withResourceType(RESOURCE_TYPE)
              .withConfiguration(mapper.valueToTree(instance.toBuilder()))
              .withCreatedIso(instance.launchTime())
              .withAccountId(account)
              .withAwsRegion(region.toString())
              .withTags(getConvertedTags(instance.tags(), mapper))
              .build();

            massageInstanceTypeAndPublicIp(data, instance, mapper, region, RESOURCE_TYPE);
            discoverBackupJobs(arn, region, data, clientCreator, logger);
            emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService()), data.toJsonNode()));
          })));
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  public void massageInstanceTypeAndPublicIp(MagpieAwsResource data,
                                             Instance instance,
                                             ObjectMapper mapper,
                                             Region region,
                                             String resourceType) {
    try {
      var instanceForUpdate = mapper.readerForUpdating(data.configuration);

      data.configuration = instanceForUpdate.readValue(mapper.convertValue(
        Map.of("instanceType", instance.instanceTypeAsString()), JsonNode.class));

      if (!StringUtils.isEmpty(instance.publicIpAddress())) {
        data.configuration = instanceForUpdate.readValue(mapper.convertValue(
          Map.of("publicIp", instance.publicIpAddress()), JsonNode.class));
      }

    } catch (IOException ex) {
      DiscoveryExceptions.onDiscoveryException(resourceType, null, region, ex);
    }
  }

  private void discoverEIPs(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = Ec2ElasticIpAddress.RESOURCE_TYPE;

    try {
      client.describeAddresses().addresses().forEach(eip -> {
        String arn = format("arn:aws:ec2:%s:%s:eip-allocation/%s", region, account, eip.allocationId());
        var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, arn)
          .withResourceName(eip.publicIp())
          .withResourceId(eip.allocationId())
          .withResourceType(RESOURCE_TYPE)
          .withConfiguration(mapper.valueToTree(eip.toBuilder()))
          .withAccountId(account)
          .withAwsRegion(region.toString())
          .withTags(getConvertedTags(eip.tags(), mapper))
          .build();

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":eip"), data.toJsonNode()));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }

  }

  private void discoverSecurityGroups(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account, Logger logger) {
    final String RESOURCE_TYPE = EC2SecurityGroup.RESOURCE_TYPE;

    try {
      client.describeSecurityGroupsPaginator().stream()
        .flatMap(r -> r.securityGroups().stream())
        .forEach(securityGroup -> {
          String arn = format("arn:aws:ec2:%s:%s:security-group/%s", region, account, securityGroup.groupId());
          var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, arn)
            .withResourceName(securityGroup.groupName())
            .withResourceId(securityGroup.groupId())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(securityGroup.toBuilder()))
            .withAccountId(account)
            .withAwsRegion(region.toString())
            .withTags(getConvertedTags(securityGroup.tags(), mapper))
            .build();

          // PROD-2758 requires that ipPermissionEgress[].ipRanges be string values.  For unknown reasons we're bringing
          // back objects at that location instead.  Pull out the cidrIP value from the object and place it under
          var egressNode = data.configuration.get("ipPermissionsEgress");
          if (egressNode instanceof ArrayNode) {
            updateIpPermissionsEgressNode((ArrayNode)egressNode, mapper);
          }

          List<EC2SecurityGroup.OwnerCIDR> cidrOwnersList =
            StreamSupport.stream(data.configuration.get("ipPermissions").spliterator(), false)
              .flatMap(perm -> StreamSupport.stream(perm.get("ipRanges").spliterator(), false))
              .map(ipRange -> ipRange.get("cidrIp").textValue())
              .filter(cidrIp -> !"0.0.0.0/0".equals(cidrIp))
              .map(cidr -> ipOwnerLookup(arn, cidr, logger, mapper))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(Collectors.toList());

          AWSUtils.update(data.supplementaryConfiguration, Map.of("ipPermissionsCidrOwners", cidrOwnersList));

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":securityGroup"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }

  }

  private void discoverNetworkAcls(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = Ec2NetworkAcl.RESOURCE_TYPE;

    try {
      client.describeNetworkAclsPaginator(DescribeNetworkAclsRequest.builder().build()).networkAcls().stream()
        .forEach(acl -> {
          String arn = format("arn:aws:ec2:%s:%s:network-acl/%s", region, account, acl.networkAclId());
          var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, arn)
            .withResourceName(acl.networkAclId())
            .withResourceId(acl.networkAclId())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(acl.toBuilder()))
            .withAccountId(account)
            .withAwsRegion(region.toString())
            .withTags(getConvertedTags(acl.tags(), mapper))
            .build();

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":NetworkAcl"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverTransitGateway(ObjectMapper mapper, Session session, Ec2Client client, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = Ec2TransitGateway.RESOURCE_TYPE;

    try {
      client.describeTransitGateways().transitGateways().forEach(transitGateway -> {
          String arn = format("arn:aws:ec2:%s:%s:transit-gateway/%s", region, account,
            transitGateway.transitGatewayId());
          var data = new MagpieAwsResource.MagpieAwsResourceBuilder(mapper, arn)
            .withResourceName(transitGateway.transitGatewayId())
            .withResourceId(transitGateway.transitGatewayId())
            .withResourceType(RESOURCE_TYPE)
            .withConfiguration(mapper.valueToTree(transitGateway.toBuilder()))
            .withAccountId(account)
            .withAwsRegion(region.toString())
            .withTags(getConvertedTags(transitGateway.tags(), mapper))
            .build();

          discoverTransit(client, data, transitGateway);

          emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(AWSDiscoveryPlugin.ID + ":TransitGateway"), data.toJsonNode()));
        });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverTransit(Ec2Client client, MagpieAwsResource data, TransitGateway transitGateway) {
    final String keyname = "transit";

    getAwsResponse(
      () -> client.describeTransitGateways(DescribeTransitGatewaysRequest.builder().build()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private JsonNode getConvertedTags(List<Tag> tags, ObjectMapper mapper) {
    return mapper.convertValue(tags.stream().collect(
      Collectors.toMap(Tag::key, Tag::value)), JsonNode.class);
  }

  /**
   * This helper function looks up the IP owner of the CIDR given to it.  It does this by removing the netmask
   * and then getting the owner's netrange of that IP via a whois call. If the netrange is less than the mask
   * passed in (eg. 205.251.233.176/29 vs 205.251.192.0/18), then the cidr passed in is wholly consumed by the
   * owner, so the function returns the organization (eg. Amazon.com, Inc. (AMAZON-4)).  Otherwise it returns a
   * empty Optional as can't be certain who the owner is because the cidr overlaps the owner's netrange.
   *
   * @param cidr to lookup the owner of
   * @return the owner of the CIDR, or empty if it's not a clear singular owner, or any number of regex mismatches
   */
  private Optional<EC2SecurityGroup.OwnerCIDR> ipOwnerLookup(String resourceARN, String cidr, Logger logger, ObjectMapper mapper) {
    JsonNode whoisResponse = null;
    try {
      // check first that we have a valid cidr
      Matcher cidrValidate = CIDR_REGEX.matcher(cidr);
      if (!cidrValidate.find()) {
        logger.warn("CIDR {} was not a valid format", cidr);
        return Optional.empty();
      }

      SubnetUtils.SubnetInfo sgSubnet = new SubnetUtils(cidr).getInfo();

      // see https://www.arin.net/resources/registry/whois/rws/api/ for more info on REST call and JSON response
      whoisResponse = whoisCall(cidr, logger, mapper);
      if (whoisResponse != null) {
        String orgName = whoisResponse.at("/net/orgRef/@name").textValue();
        String customerName = whoisResponse.at("/net/customerRef/@name").textValue();
        String ownername = orgName != null ? orgName : customerName;

        // If the CIDR is invalid then it resolves to a single host netmask of 255.255.255.255 vs an address
        // range to check. We set the 'isBadCIDR' flag in this case, but otherwise continue to get the owner
        // netblock of that single IP (if possible, bypassing the 'isSgInWhoisNetblock' check as there is no range to check)
        final boolean isBadCidr = SINGLE_HOST_NETMASK.equals(sgSubnet.getNetmask());

        JsonNode netblocks = whoisResponse.requiredAt("/net/netBlocks");
        JsonNode netblock = netblocks.requiredAt("/netBlock");
        if (!netblock.isArray()) {
          if (isBadCidr || isSgInWhoisNetblock(sgSubnet, netblock)) {
            String startAddress = netblock.requiredAt("/startAddress/$").textValue();
            String cidrLength = netblock.requiredAt("/cidrLength/$").textValue();
            return Optional.of(new EC2SecurityGroup.OwnerCIDR(cidr, ownername, startAddress + "/" + cidrLength, isBadCidr));
          }
        } else {
          for (final JsonNode n : netblock) {

            if (isBadCidr || isSgInWhoisNetblock(sgSubnet, n)) {
              String startAddress = n.requiredAt("/startAddress/$").textValue();
              String cidrLength = n.requiredAt("/cidrLength/$").textValue();
              return Optional.of(new EC2SecurityGroup.OwnerCIDR(cidr, ownername, startAddress + "/" + cidrLength, isBadCidr));
            }
          }
        }
      }

      // if we've fallen this far though, we either don't have a match, or the CIDRs that did match are not
      // wholly consumed by an owners netblock.  This isnt necessary to resolve in the product but useful
      // to have some knowledge of how often it crops up because it _should_ be unusual, so send a segment event.
      if (whoisResponse != null) {
        logger.info("NULL whois lookup - https://openraven.atlassian.net/browse/ENG-5286");
        //NOTE: Disabled for now in preference of looking at other discovery error -
                /*
                 this usually means that we don't have a contiguous netblock match for any of the whois owner netblocks
                 useful to know this, but we dont need to alert on it, so send a segment (vs sentry) event.
                var propertyMap = ImmutableMap
                        .of("discovery-session", discoverySession,
                                "Resource", resourceARN,
                                "CIDR", cidr,
                                "Whois Response", whoisResponse.toPrettyString());
                discoveryServices.sendAnalyticsEvent("CIDR-lookup-noncontiguous", propertyMap);

                Sentry.capture(new EventBuilder().withMessage("Whois lookup was null")
                        .withMessage("Null response from whois")
                        .withLevel(Event.Level.INFO)
                        .withExtra("Resource", resourceARN)
                        .withExtra("CIDR", cidr));
                        */
      }

      return Optional.empty();

    } catch (EC2SecurityGroup.WhoisLookupException e) {
      logger.error("Error while getting IP owner for {}", cidr, e);
      //TODO: really want to move this sentry event into shared discovery exceptions
      //but the refactor of that class is in an active branch - will pick it up later
      Sentry.capture(new EventBuilder().withMessage("WHOIS owner lookup exception while running EC2 SecurityGroup discovery")
        .withLevel(Event.Level.ERROR)
        .withExtra("Resource", resourceARN)
        .withExtra("CIDR", cidr)
        .withExtra("Reason", e)
        .withSentryInterface(new ExceptionInterface(e)));

      return Optional.empty();
    } catch (IllegalArgumentException e) {
      logger.warn("Malformed whois output: <<{}>>", whoisResponse, e);
      //TODO: really want to move this sentry event into shared discovery exceptions
      //but the refactor of that class is in an active branch - will pick it up later
      Sentry.capture(new EventBuilder().withMessage("Malformed whois output")
        .withLevel(Event.Level.ERROR)
        .withExtra("Resource", resourceARN)
        .withExtra("CIDR", cidr)
        .withSentryInterface(new ExceptionInterface(e)));
      return Optional.empty();
    }
  }

  @Nullable
  private JsonNode whoisCall(String cidr, Logger logger, ObjectMapper mapper) throws EC2SecurityGroup.WhoisLookupException {
    // temporary caching - may need to externalize this as it will get recreated on each discovery run
    if (whoisCache.containsKey(cidr)) {
      return whoisCache.get(cidr);
    }

    Matcher ipFromCidr = CIDR_REGEX.matcher(cidr);
    if (!ipFromCidr.find()) {
      logger.warn("CIDR {} was not a valid format", cidr);
      throw new EC2SecurityGroup.WhoisLookupException(String.format("CIDR %s was not a valid format", cidr));
    }
    String ip = ipFromCidr.group(1);

    // negative: * error:141A318A:SSL routines:tls_process_ske_dhe:dh key too small
    @SuppressWarnings("HttpUrlsUsage")
    String whoisReq = "http://whois.arin.net/rest/ip/" + ip;
    HttpResponse<String> response = Unirest.get(whoisReq)
      .header(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString())
      .asString();

    if (response.getStatus() == HttpStatus.OK) {
      try {
        JsonNode whoisResp = mapper.readTree(response.getBody());
        if (!whoisResp.at("/net/resources/limitExceeded/$").asBoolean()) {
          whoisCache.put(cidr, whoisResp);
          return whoisResp;
        } else {
          Sentry.capture(new EventBuilder().withMessage("whois look rate limited")
            .withLevel(Event.Level.WARNING)
            .withExtra("CIDR", cidr));
          return null;
        }
      } catch (JsonProcessingException e) {
        throw (EC2SecurityGroup.WhoisLookupException) (new EC2SecurityGroup.WhoisLookupException("Error processing whois response to " + whoisReq).initCause(e));
      }
    } else {
      logger.warn("Bogus whois repsonse: {}", response);
    }

    return null;
  }

  /**
   * check that the IP range of the SG's CIDR fits within this (one of) the netblocks that the
   * whois owner has.  Otherwise the SG range maybe too permissive and span out to other owners.
   *
   * @param sgSubnet is the subnet info for the security group
   * @param netblock is a netblock fragment from a whois lookup
   * @return true if the SG subnet is completely contained within the netblock
   */
  private boolean isSgInWhoisNetblock(SubnetUtils.SubnetInfo sgSubnet, JsonNode netblock) {
    long sgLowAddressInt = ipToLong(sgSubnet.getLowAddress());
    long sgHighAddressInt = ipToLong(sgSubnet.getHighAddress());

    long whoisLowAddressInt = ipToLong(netblock.requiredAt("/startAddress/$").textValue());
    long whoisHighAddressInt = ipToLong(netblock.requiredAt("/endAddress/$").textValue());

    return (sgLowAddressInt >= whoisLowAddressInt) && (sgHighAddressInt <= whoisHighAddressInt);
  }

  private long ipToLong(String ipAddress) {
    long result = 0;
    String[] ipAddressInArray = ipAddress.split("\\.");

    for (int i = 3; i >= 0; i--) {
      long ip = Long.parseLong(ipAddressInArray[3 - i]);

      //left shifting 24,16,8,0 and bitwise OR
      result |= ip << (i * 8);
    }

    return result;
  }

  // Fix for https://openraven.atlassian.net/browse/PROD-2758,
  // ipPermissionsEgress[]->ipRanges[] needs to be a CIDR string, not an object containing the string. Unsure how
  // this changed between AWSDiscovery and Magpie
  private void updateIpPermissionsEgressNode(ArrayNode ipPermissionsEgressNode, ObjectMapper mapper) {
    for (int i = 0 ; i < ipPermissionsEgressNode.size() ; i++) {
      ObjectNode objNode = (ObjectNode)ipPermissionsEgressNode.get(i);
      ArrayNode ipRangesNode = (ArrayNode) objNode.get("ipRanges");
      for (int j = 0 ; j < ipRangesNode.size() ; j++) {
        var child = (ObjectNode) ipRangesNode.get(j);
        ipRangesNode.set(j, mapper.valueToTree(child.get("cidrIp")));
      }
    }
  }
}
