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
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.api.Session;
import io.openraven.magpie.plugins.aws.discovery.AWSUtils;
import io.openraven.magpie.plugins.aws.discovery.VersioningEmitterWrapper;
import org.slf4j.Logger;
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

  private final List<LocalDiscovery> discoveryMethods = List.of(
    this::discoverCredentialsReport,
    this::discoverAccounts,
    this::discoverGroups,
    this::discoverUsers,
    this::discoverRoles,
    this::discoverPolicies
  );

  @FunctionalInterface
  interface LocalDiscovery {
    void discover(IamClient client, ObjectMapper mapper, Session session, Region region, VersioningEmitterWrapper emitter, Logger logger);
  }

  @Override
  public String service() {
    return SERVICE;
  }

  @Override
  public List<Region> getSupportedRegions() {
    return IamClient.serviceMetadata().regions();
  }

  @Override
  public void discover(ObjectMapper mapper, Session session, Region region, VersioningEmitterWrapper emitter, Logger logger) {
    final var client = IamClient.builder().region(region).build();

    discoveryMethods.forEach(dm -> dm.discover(client, mapper, session, region, emitter, logger));
  }

  private void discoverRoles(IamClient client, ObjectMapper mapper, Session session, Region region, VersioningEmitterWrapper emitter, Logger logger) {
    getAwsResponse(
      () -> client.listRolesPaginator().roles(),
      (resp) -> resp.forEach(role -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", role.toBuilder());
        data.put("region", region.toString());

        discoverAttachedPolicies(client, data, role);
        discoverInlinePolicies(client, data, role);

        AWSUtils.update(data, Map.of("tags", mapper.convertValue(role.tags().stream().collect(
          Collectors.toMap(Tag::key, Tag::value)), JsonNode.class)));

        emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":role"), data));
      }),
      (noresp) -> logger.error("Failed to get roles in {}", region)
    );
  }

  private void discoverInlinePolicies(IamClient client, ObjectNode data, Role role) {
    List<ImmutableMap<String, String>> inlinePolicies = new ArrayList<>();

    getAwsResponse(
      () -> client.listRolePoliciesPaginator(ListRolePoliciesRequest.builder().roleName(role.roleName()).build()).policyNames().stream()
        .map(r -> client.getRolePolicy(GetRolePolicyRequest.builder().roleName(role.roleName()).policyName(r).build()))
        .collect(Collectors.toList()),
      (resp) -> resp.forEach( policy ->  inlinePolicies.add(ImmutableMap.of(
        "name", policy.policyName(),
        "policyDocument", policy.policyDocument()))),
      (noresp) -> {
      }
    );

    AWSUtils.update(data, Map.of("inlinePolicies", inlinePolicies));
  }

  private void discoverAttachedPolicies(IamClient client, ObjectNode data, Role role) {
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

    AWSUtils.update(data, Map.of("attachedPolicies", attachedPolicies));
  }

  private void discoverPolicies(IamClient client, ObjectMapper mapper, Session session, Region region, VersioningEmitterWrapper emitter, Logger logger) {
    getAwsResponse(
      () -> client.listPoliciesPaginator(builder -> builder.scope(PolicyScopeType.LOCAL)).policies(),
      (resp) -> resp.forEach(policy -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", policy.toBuilder());
        data.put("region", region.toString());

        discoverPolicyDocument(client, data, policy);

        emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":policy"), data));
      }),
      (noresp) -> logger.error("Failed to get policies in {}", region)
    );
  }

  private void discoverPolicyDocument(IamClient client, ObjectNode data, Policy policy) {
    getAwsResponse(
      () -> client.listPolicyVersionsPaginator(ListPolicyVersionsRequest.builder().policyArn(policy.arn()).build()),
      (resp) -> resp.forEach(policyVersionsResponse -> {
        var currentPolicy = policyVersionsResponse.versions()
          .stream()
          .filter(PolicyVersion::isDefaultVersion)
          .findFirst();
        currentPolicy.ifPresent(policyVersion -> getAwsResponse(
          () -> client.getPolicyVersion(GetPolicyVersionRequest.builder().policyArn(policy.arn()).versionId(policyVersion.versionId()).build()),
          (innerResp) -> AWSUtils.update(data, Map.of("attachedPolicies", Map.of("policyDocument", innerResp.policyVersion().document()))),
          (innerNoresp) -> {
          }
        ));
      }),
      (noresp) -> {
      }
    );
  }

  private void discoverUsers(IamClient client, ObjectMapper mapper, Session session, Region region, VersioningEmitterWrapper emitter, Logger logger) {
    getAwsResponse(
      () -> client.listUsersPaginator().users(),
      (resp) -> resp.forEach(user -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", user.toBuilder());
        data.put("region", region.toString());

        discoverGroupsForUser(client, data, user);
        discoverAttachedUserPolicies(client, data, user);
        discoverUserPolicies(client, data, user);
        discoverUserMFADevices(client, data, user);

        AWSUtils.update(data, Map.of("tags", mapper.convertValue(user.tags().stream().collect(
          Collectors.toMap(Tag::key, Tag::value)), JsonNode.class)));

        emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":user"), data));
      }),
      (noresp) -> logger.error("Failed to get users in {}", region)
    );
  }

  private void discoverGroupsForUser(IamClient client, ObjectNode data, User user) {
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

    AWSUtils.update(data, Map.of("groups", attachedPolicies));
  }

  private void discoverAttachedUserPolicies(IamClient client, ObjectNode data, User user) {
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

    AWSUtils.update(data, Map.of("attachedPolicies", attachedPolicies));
  }

  private void discoverUserPolicies(IamClient client, ObjectNode data, User user) {
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

    AWSUtils.update(data, Map.of("userPolicies", inlinePolicies));
  }

  private void discoverUserMFADevices(IamClient client, ObjectNode data, User user) {
    String keyname = "mfaDevices";

    getAwsResponse(
      () -> client.listMFADevicesPaginator(ListMfaDevicesRequest.builder().userName(user.userName()).build())
        .stream()
        .map(r -> r.toBuilder())
        .collect(Collectors.toList()),
      (resp) -> AWSUtils.update(data, Map.of(keyname, resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname, noresp))
    );
  }

  private void discoverGroups(IamClient client, ObjectMapper mapper, Session session, Region region, VersioningEmitterWrapper emitter, Logger logger) {
    getAwsResponse(
      () -> client.listGroups().groups(),
      (resp) -> resp.forEach(group -> {
        var data = mapper.createObjectNode();
        data.putPOJO("configuration", group.toBuilder());
        data.put("region", region.toString());

        discoverGroupInlinePolicies(client,data,group);
        discoverGroupAttachedPolicies(client,data,group);

        emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":group"), data));
      }),
      (noresp) -> logger.error("Failed to get groups in {}", region)
    );
  }

  private void discoverGroupInlinePolicies(IamClient client, ObjectNode data, Group group) {
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

    AWSUtils.update(data, Map.of("inlinePolicies", inlinePolicies));
  }

  private void discoverGroupAttachedPolicies(IamClient client, ObjectNode data, Group group) {
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

    AWSUtils.update(data, Map.of("attachedPolicies", attachedPolicies));
  }

  private void discoverAccounts(IamClient client, ObjectMapper mapper, Session session, Region region, VersioningEmitterWrapper emitter, Logger logger) {
    var data = mapper.createObjectNode();
    data.put("region", region.toString());

    discoverAccountAlias(client, data);
    discoverAccountPasswordPolicy(client, data);
    discoverAccountSummary(client, data);
    discoverVirtualMFADevices(client, data);

    emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":account"), data));
  }

  private void discoverAccountAlias(IamClient client, ObjectNode data) {
    final String keyname = "alias";
    getAwsResponse(
      () -> client.listAccountAliases().accountAliases().stream().findFirst().orElse(null),
      (resp) -> AWSUtils.update(data, Map.of(keyname,resp)),
      (noresp) -> {
      }
    );
  }

  private void discoverAccountPasswordPolicy(IamClient client, ObjectNode data) {
    final String keyname = "PasswordPolicy";

    getAwsResponse(
      () -> client.getAccountPasswordPolicy().passwordPolicy(),
      (resp) -> AWSUtils.update(data, Map.of(keyname,resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname,noresp))
    );
  }

  private void discoverAccountSummary(IamClient client, ObjectNode data) {
    final String keyname = "summaryMap";

    getAwsResponse(
      () -> client.getAccountSummary().summaryMapAsStrings(),
      (resp) -> AWSUtils.update(data, Map.of(keyname,resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname,noresp))
    );
  }

  private void discoverVirtualMFADevices(IamClient client, ObjectNode data) {
    final String keyname = "virtualMFADevices";

    getAwsResponse(
      () -> client.listVirtualMFADevices().toBuilder(),
      (resp) -> AWSUtils.update(data, Map.of(keyname,resp)),
      (noresp) -> AWSUtils.update(data, Map.of(keyname,noresp))
    );
  }

  private void discoverCredentialsReport(IamClient client, ObjectMapper mapper, Session session, Region region, VersioningEmitterWrapper emitter, Logger logger) {
    getAwsResponse(
      () -> generateCredentialReport(client),
      (resp) -> {
        if (resp) {
          processCredentialsReport(client, mapper, session, region, emitter);
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

  private void processCredentialsReport(IamClient client, ObjectMapper mapper, Session session, Region region, VersioningEmitterWrapper emitter) {
    var report = client.getCredentialReport();
    String reportContent = report.content().asUtf8String();

    String[] reportLines = reportContent.split(System.getProperty("line.separator"));

    for(int i = 1; i < reportLines.length; i++) {
      var data = mapper.createObjectNode();
      data.put("region", region.toString());

      var credential = new IAMCredential(reportLines[i]);

      AWSUtils.update(data, Map.of("report",mapper.valueToTree(credential)));

      emitter.emit(new MagpieEnvelope(session, List.of(fullService() + ":credentialsReport"), data));
    }
  }
}
