/*-
 * #%L
 * Magpie API
 * %%
 * Copyright (C) 2021 Open Raven Inc
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
package io.openraven.magpie.data.aws.dynamodb;

import io.openraven.magpie.data.aws.AWSResource;
import io.openraven.magpie.data.aws.shared.PayloadUtils;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

@jakarta.persistence.Entity
@jakarta.persistence.Inheritance(strategy = jakarta.persistence.InheritanceType.TABLE_PER_CLASS)
@jakarta.persistence.Table(name = DynamoDbTable.TABLE_NAME)
public class DynamoDbTable extends AWSResource {
  protected static final String TABLE_NAME = "awsdynamodbtable";
  public static final String RESOURCE_TYPE = "AWS::DynamoDB::Table";

  public DynamoDbTable() {this.resourceType = RESOURCE_TYPE;}

  public DynamoDbTable(String account, String region, TableDescription table) {
    this.awsAccountId = account;
    this.awsRegion = region;
    this.resourceName = table.tableName();
    this.resourceId = table.tableId();
    this.arn = table.tableArn();
    this.createdIso = table.creationDateTime();
    this.configuration = PayloadUtils.update(table);
    this.resourceType = RESOURCE_TYPE;
  }
}
