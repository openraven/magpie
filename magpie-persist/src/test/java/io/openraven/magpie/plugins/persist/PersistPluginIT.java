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

package io.openraven.magpie.plugins.persist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.data.aws.AWSResource;
import io.openraven.magpie.data.aws.accounts.IamGroup;
import io.openraven.magpie.plugins.persist.config.PostgresPersistenceProvider;
import io.openraven.magpie.plugins.persist.migration.FlywayMigrationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainerProvider;

import javax.persistence.EntityManager;
import java.util.List;

import static io.openraven.magpie.plugins.persist.TestUtils.getResourceAsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PersistPluginIT {

  private static final String SELECT_GROUP_TABLE = "SELECT a FROM IamGroup a";
  private static final String SELECT_ASSETS_TABLE = "SELECT a FROM AssetModel a";

  private static EntityManager entityManager;
  private static PersistConfig persistConfig;
  private static final PersistPlugin persistPlugin = new PersistPlugin();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeAll
  static void setup() {
    var postgreSQLContainerProvider = new PostgreSQLContainerProvider();
    var jdbcDatabaseContainer = postgreSQLContainerProvider.newInstance();
    jdbcDatabaseContainer.withUrlParam("stringtype", "unspecified").start();

    persistConfig = new PersistConfig();
    persistConfig.setHostname("localhost");
    persistConfig.setDatabaseName(jdbcDatabaseContainer.getDatabaseName());
    persistConfig.setPort(String.valueOf(jdbcDatabaseContainer.getFirstMappedPort()));
    persistConfig.setUser(jdbcDatabaseContainer.getUsername());
    persistConfig.setPassword(jdbcDatabaseContainer.getUsername());

    entityManager = PostgresPersistenceProvider.getEntityManager(persistConfig);

    FlywayMigrationService.initiateDBMigration(persistConfig);

    persistPlugin.init(persistConfig, LoggerFactory.getLogger(PersistPluginIT.class));
  }

  @Test
  void whenPersistPluginProcessEnvelopeDataShouldBeSaved() throws Exception {
    // given
    ObjectNode contents = objectMapper.readValue(
      getResourceAsString("/documents/envelope-content.json"), ObjectNode.class);
    MagpieEnvelope magpieEnvelope = new MagpieEnvelope();
    magpieEnvelope.setContents(contents);

    // when
    persistPlugin.accept(magpieEnvelope);

    // then
    List<IamGroup> assets = queryIamGroupTable();
    assertEquals (1, assets.size());
    assertAsset(assets.get(0));

    List<AssetModel> assetModels = queryAssetsTable();
    assertEquals(1, assetModels.size());
  }

  @Test
  void whenProcessEnvelopeDataShouldBeSavedWithUpsert() throws Exception {
    // given

    ObjectNode updatedContent = objectMapper.readValue(
      getResourceAsString("/documents/outdated-envelope-content.json"), ObjectNode.class);
    MagpieEnvelope outdatedMagpieEnvelope = new MagpieEnvelope();
    outdatedMagpieEnvelope.setContents(updatedContent);

    // when
    persistPlugin.accept(outdatedMagpieEnvelope);

    // then
    List<IamGroup> assets = queryIamGroupTable();
    assertEquals(1, assets.size());
    assertEquals("outdated-resource", assets.get(0).resourceName);

    // given
    ObjectNode content = objectMapper.readValue(
      getResourceAsString("/documents/envelope-content.json"), ObjectNode.class);
    MagpieEnvelope magpieEnvelope = new MagpieEnvelope();
    magpieEnvelope.setContents(content);

    persistPlugin.accept(magpieEnvelope);
    // then
    List<IamGroup> updatedAssets = queryIamGroupTable();
    assertEquals(1, updatedAssets.size());
    assertAsset(updatedAssets.get(0));

    List<AssetModel> assetModels = queryAssetsTable();
    assertEquals(1, assetModels.size());
  }

  private void assertAsset(AWSResource awsResource) {
    assertEquals("4jUz_CPXMG-Z7f8oJltkPg", awsResource.documentId);
    assertEquals("arn:aws:iam::000000000000:group/Accountants", awsResource.arn);
    assertEquals("Accountants", awsResource.resourceName);
    assertEquals("y9xomssf3o582439fxep", awsResource.resourceId);
    assertEquals("AWS::IAM::Group", awsResource.resourceType);
    assertEquals("us-west-1", awsResource.awsRegion);
    assertEquals("account", awsResource.awsAccountId);
    assertNull(awsResource.createdIso);
    assertEquals("2021-06-23T09:44:50.397706Z", awsResource.updatedIso.toString());
    assertNull(awsResource.discoverySessionId);
    assertNull(awsResource.maxSizeInBytes);
    assertNull(awsResource.sizeInBytes);
    assertEquals("{\"arn\":\"arn:aws:iam::000000000000:group/Accountants\",\"path\":\"/\",\"groupId\":\"y9xomssf3o582439fxep\",\"groupName\":\"Accountants\",\"createDate\":null}",
      awsResource.configuration.toString());
    assertEquals("{\"inlinePolicies\":[{\"name\":\"inlineDataAccess\",\"policyDocument\":\"{\\\"Version\\\":\\\"2012-10-17\\\",\\\"Statement\\\":[{\\\"Effect\\\":\\\"Deny\\\",\\\"Action\\\":[\\\"dynamodb:DeleteItem\\\",\\\"dynamodb:GetItem\\\"],\\\"Resource\\\":\\\"*\\\"}]}\"}],\"attachedPolicies\":[{\"arn\":\"arn:aws:iam::000000000000:policy/managedDataAccess\",\"name\":\"managedDataAccess\"}]}",
      awsResource.supplementaryConfiguration.toString());
    assertEquals("{}", awsResource.tags.toString());
    assertEquals("{}", awsResource.discoveryMeta.toString());
  }

  private List<IamGroup> queryIamGroupTable() {
    entityManager.clear();
    return entityManager.createQuery(SELECT_GROUP_TABLE, IamGroup.class)
      .getResultList();
  }

  private List<AssetModel> queryAssetsTable() {
    entityManager.clear();
    return entityManager.createQuery(SELECT_ASSETS_TABLE, AssetModel.class)
      .getResultList();
  }
}
