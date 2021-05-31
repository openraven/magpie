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

import com.datastax.oss.driver.shaded.guava.common.collect.ImmutableMap;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openraven.magpie.api.Emitter;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSResource;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.DiscoveryExceptions;
import io.openraven.magpie.plugins.aws.discovery.VersionedMagpieEnvelopeProvider;
import org.slf4j.Logger;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.openraven.magpie.plugins.aws.discovery.AWSUtils.getAwsResponse;

public class IAMDiscovery implements AWSDiscovery {

  private static final String SERVICE = "iam";

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return IamClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    final var client = IamClient.builder().region(region).build();

    discoverCredentialsReport(client, mapper, session, region, emitter, logger, account);
    discoverAccount(client, mapper, session, region, emitter, account);
    discoverGroups(client, mapper, session, region, emitter, account);
    discoverUsers(client, mapper, session, region, emitter, account);
    discoverRoles(client, mapper, session, region, emitter, account);
    discoverPolicies(client, mapper, session, region, emitter, account);
  }

  private void discoverRoles(IamClient client, ObjectMapper mapper, Session session, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::IAM::Role";

    try {
      client.listRolesPaginator().roles().forEach(role -> {
        var data = new AWSResource(role.toBuilder(), region.toString(), account, mapper);
        data.arn = role.arn();
        data.resourceId = role.roleId();
        data.resourceName = role.roleName();
        data.resourceType = RESOURCE_TYPE;
        data.createdIso = role.createDate();

        discoverAttachedPolicies(client, data, role);
        discoverInlinePolicies(client, data, role);

        AWSUtils.update(data.tags, Map.of("tags", mapper.convertValue(role.tags().stream().collect(
          Collectors.toMap(Tag::key, Tag::value)), JsonNode.class)));

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":role"), data.toJsonNode(mapper)));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverInlinePolicies(IamClient client, AWSResource data, Role role) {
    List<ImmutableMap<String, String>> inlinePolicies = new ArrayList<>();

    getAwsResponse(
      () -> client.listRolePoliciesPaginator(ListRolePoliciesRequest.builder().roleName(role.roleName()).build()).policyNames().stream()
        .map(r -> client.getRolePolicy(GetRolePolicyRequest.builder().roleName(role.roleName()).policyName(r).build()))
        .collect(Collectors.toList()),
      (resp) -> resp.forEach(policy -> inlinePolicies.add(ImmutableMap.of(
        "name", policy.policyName(),
        "policyDocument", policy.policyDocument()))),
      (noresp) -> {
      }
    );

    AWSUtils.update(data.supplementaryConfiguration, Map.of("inlinePolicies", inlinePolicies));
  }

  private void discoverAttachedPolicies(IamClient client, AWSResource data, Role role) {
    List<ImmutableMap<String, String>> attachedPolicies = new ArrayList<>();

    getAwsResponse(
      () -> client.listAttachedRolePoliciesPaginator(ListAttachedRolePoliciesRequest.builder().roleName(role.roleName()).build()).attachedPolicies(),
      (resp) -> resp.forEach(attachedRolePolicy ->
        attachedPolicies.add(ImmutableMap.of(
          "name", attachedRolePolicy.policyName(),
          "arn", attachedRolePolicy.policyArn()))),
      (noresp) -> {
      }
    );

    AWSUtils.update(data.supplementaryConfiguration, Map.of("attachedPolicies", attachedPolicies));
  }

  private void discoverPolicies(IamClient client, ObjectMapper mapper, Session session, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::IAM::Policy";

    try {
      client.listPoliciesPaginator().policies().forEach(policy -> {
        var data = new AWSResource(policy.toBuilder(), region.toString(), account, mapper);
        data.arn = policy.arn();
        data.resourceId = policy.policyId();
        data.resourceName = policy.policyName();
        data.resourceType = RESOURCE_TYPE;
        data.createdIso = policy.createDate();

        discoverPolicyDocument(client, data, policy);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":policy"), data.toJsonNode(mapper)));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverPolicyDocument(IamClient client, AWSResource data, Policy policy) {
    getAwsResponse(
      () -> client.listPolicyVersionsPaginator(ListPolicyVersionsRequest.builder().policyArn(policy.arn()).build()),
      (resp) -> resp.forEach(policyVersionsResponse -> {
        var currentPolicy = policyVersionsResponse.versions()
          .stream()
          .filter(PolicyVersion::isDefaultVersion)
          .findFirst();
        currentPolicy.ifPresent(policyVersion -> getAwsResponse(
          () -> client.getPolicyVersion(GetPolicyVersionRequest.builder().policyArn(policy.arn()).versionId(policyVersion.versionId()).build()),
          (innerResp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of("attachedPolicies", Map.of("policyDocument", innerResp.policyVersion().document()))),
          (innerNoresp) -> {
          }
        ));
      }),
      (noresp) -> {
      }
    );
  }

  private void discoverUsers(IamClient client, ObjectMapper mapper, Session session, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::IAM::User";

    try {
      client.listUsersPaginator().users().forEach(user -> {
        var data = new AWSResource(user.toBuilder(), region.toString(), account, mapper);
        data.arn = user.arn();
        data.resourceId = user.userId();
        data.resourceName = user.userName();
        data.resourceType = RESOURCE_TYPE;
        data.createdIso = user.createDate();

        discoverGroupsForUser(client, data, user);
        discoverAttachedUserPolicies(client, data, user);
        discoverUserPolicies(client, data, user);
        discoverUserMFADevices(client, data, user);

        AWSUtils.update(data.tags, mapper.convertValue(user.tags().stream().collect(Collectors.toMap(Tag::key, Tag::value)), JsonNode.class));

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":user"), data.toJsonNode(mapper)));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverGroupsForUser(IamClient client, AWSResource data, User user) {
    List<ImmutableMap<String, String>> attachedPolicies = new ArrayList<>();

    getAwsResponse(
      () -> client.listGroupsForUserPaginator(ListGroupsForUserRequest.builder().userName(user.userName()).build()).groups(),
      (resp) -> resp.forEach(group ->
        attachedPolicies.add(ImmutableMap.of(
          "name", group.groupName(),
          "arn", group.arn()))),
      (noresp) -> {
      }
    );

    AWSUtils.update(data.supplementaryConfiguration, Map.of("groups", attachedPolicies));
  }

  private void discoverAttachedUserPolicies(IamClient client, AWSResource data, User user) {
    List<ImmutableMap<String, String>> attachedPolicies = new ArrayList<>();

    getAwsResponse(
      () -> client.listAttachedUserPoliciesPaginator(ListAttachedUserPoliciesRequest.builder().userName(user.userName()).build()).attachedPolicies(),
      (resp) -> resp.forEach(attachUserPolicy ->
        attachedPolicies.add(ImmutableMap.of(
          "name", attachUserPolicy.policyName(),
          "arn", attachUserPolicy.policyArn()))),
      (noresp) -> {
      }
    );

    AWSUtils.update(data.supplementaryConfiguration, Map.of("attachedPolicies", attachedPolicies));
  }

  private void discoverUserPolicies(IamClient client, AWSResource data, User user) {
    List<ImmutableMap<String, String>> inlinePolicies = new ArrayList<>();

    getAwsResponse(
      () -> client.listUserPoliciesPaginator(ListUserPoliciesRequest.builder().userName(user.userName()).build()).policyNames(),
      (resp) -> resp.forEach(policyName -> getAwsResponse(
        () -> client.getUserPolicy(GetUserPolicyRequest.builder().userName(user.userName()).policyName(policyName).build()),
        (innerResp) -> inlinePolicies.add(ImmutableMap.of(
          "name", innerResp.policyName(),
          "policyDocument", innerResp.policyDocument())),
        (innerNoresp) -> {
        }
      )),
      (noresp) -> {
      }
    );

    AWSUtils.update(data.supplementaryConfiguration, Map.of("userPolicies", inlinePolicies));
  }

  private void discoverUserMFADevices(IamClient client, AWSResource data, User user) {
    String keyname = "mfaDevices";

    getAwsResponse(
      () -> client.listMFADevicesPaginator(ListMfaDevicesRequest.builder().userName(user.userName()).build())
        .stream()
        .map(ListMfaDevicesResponse::toBuilder)
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverGroups(IamClient client, ObjectMapper mapper, Session session, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::IAM::Group";

    try {
      client.listGroups().groups().forEach(group -> {
        var data = new AWSResource(group.toBuilder(), region.toString(), account, mapper);
        data.arn = group.arn();
        data.resourceId = group.groupId();
        data.resourceName = group.groupName();
        data.resourceType = RESOURCE_TYPE;
        data.createdIso = group.createDate();

        discoverGroupInlinePolicies(client, data, group);
        discoverGroupAttachedPolicies(client, data, group);

        emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":group"), data.toJsonNode(mapper)));
      });
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException(RESOURCE_TYPE, null, region, ex);
    }
  }

  private void discoverGroupInlinePolicies(IamClient client, AWSResource data, Group group) {
    List<ImmutableMap<String, String>> inlinePolicies = new ArrayList<>();

    getAwsResponse(
      () -> client.listGroupPoliciesPaginator(ListGroupPoliciesRequest.builder().groupName(group.groupName()).build()).policyNames().stream()
        .map(r -> client.getGroupPolicy(GetGroupPolicyRequest.builder().groupName(group.groupName()).policyName(r).build())),
      (resp) -> resp.forEach(rolePolicy -> inlinePolicies.add(ImmutableMap.of(
        "name", rolePolicy.policyName(),
        "policyDocument", rolePolicy.policyDocument()))
      ),
      (noresp) -> {
      }
    );

    AWSUtils.update(data.supplementaryConfiguration, Map.of("inlinePolicies", inlinePolicies));
  }

  private void discoverGroupAttachedPolicies(IamClient client, AWSResource data, Group group) {
    List<ImmutableMap<String, String>> attachedPolicies = new ArrayList<>();

    getAwsResponse(
      () -> client.listAttachedGroupPoliciesPaginator(ListAttachedGroupPoliciesRequest.builder().groupName(group.groupName()).build()).attachedPolicies(),
      (resp) -> resp.forEach(attachedGroupPolicy ->
        attachedPolicies.add(ImmutableMap.of(
          "name", attachedGroupPolicy.policyName(),
          "arn", attachedGroupPolicy.policyArn()))),
      (noresp) -> {
      }
    );

    AWSUtils.update(data.supplementaryConfiguration, Map.of("attachedPolicies", attachedPolicies));
  }

  private void discoverAccount(IamClient client, ObjectMapper mapper, Session session, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::IAM::Account";

    try {
      var accountSummary = client.getAccountSummary();
      var data = new AWSResource(accountSummary.summaryMapAsStrings(), region.toString(), account, mapper);
      data.resourceType = RESOURCE_TYPE;
      data.arn = RESOURCE_TYPE + ":" + region + ":" + account;

      discoverAccountAlias(client, data);
      discoverAccountPasswordPolicy(client, data);
      discoverVirtualMFADevices(client, data);

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":account"), data.toJsonNode(mapper)));
    } catch (SdkServiceException | SdkClientException ex) {
      DiscoveryExceptions.onDiscoveryException("Account", null, region, ex);
    }
  }

  private void discoverAccountAlias(IamClient client, AWSResource data) {
    getAwsResponse(
      () -> client.listAccountAliases().accountAliases().stream().findFirst().orElse(null),
      (resp) -> data.resourceName = resp,
      (noresp) -> {
      }
    );
  }

  private void discoverAccountPasswordPolicy(IamClient client, AWSResource data) {
    final String keyname = "passwordPolicy";

    getAwsResponse(
      () -> client.getAccountPasswordPolicy().passwordPolicy(),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverVirtualMFADevices(IamClient client, AWSResource data) {
    final String keyname = "virtualMFADevices";

    getAwsResponse(
      () -> client.listVirtualMFADevices().toBuilder(),
      (resp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data.supplementaryConfiguration, Map.of(keyname, noresp))
    );
  }

  private void discoverCredentialsReport(IamClient client, ObjectMapper mapper, Session session, Region region, Emitter emitter, Logger logger, String account) {
    getAwsResponse(
      () -> generateCredentialReport(client),
      (resp) -> {
        if (resp) {
          processCredentialsReport(client, mapper, session, region, emitter, account);
        } else {
          logger.error("Failed to generate credentialsReport in {}", region.id());
        }
      },
      (noresp) -> logger.error("Failed to get credentialsReport in {}", region.id())
    );
  }

  private boolean generateCredentialReport(IamClient client) {
    String status;
    int numTries = 0;

    status = client.generateCredentialReport().stateAsString();
    while (!status.equals("COMPLETE") && numTries < 5) {

      try {
        TimeUnit.SECONDS.sleep(30);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
      status = client.generateCredentialReport().stateAsString();
      numTries++;
    }

    return status.equals("COMPLETE");
  }

  private void processCredentialsReport(IamClient client, ObjectMapper mapper, Session session, Region region, Emitter emitter, String account) {
    final String RESOURCE_TYPE = "AWS::IAM::CredentialsReport";

    var report = client.getCredentialReport();
    String reportContent = report.content().asUtf8String();

    String[] reportLines = reportContent.split(System.getProperty("line.separator"));

    for (int i = 1; i < reportLines.length; i++) {
      var credential = new IAMCredential(reportLines[i]);
      var data = new AWSResource(mapper.valueToTree(credential), region.toString(), account, mapper);
      data.arn = credential.arn;
      data.resourceId = credential.arn;
      data.resourceName = credential.user;
      data.resourceType = RESOURCE_TYPE;

      emitter.emit(VersionedMagpieEnvelopeProvider.create(session, List.of(fullService() + ":credentialsReport"), data.toJsonNode(mapper)));
    }
  }
}
