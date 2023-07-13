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
package io.openraven.magpie.data.aws.qldb;

import io.openraven.magpie.data.aws.AWSResource;
import io.openraven.magpie.data.aws.shared.PayloadUtils;
import software.amazon.awssdk.services.qldb.model.DescribeLedgerResponse;

@jakarta.persistence.Entity
@jakarta.persistence.Inheritance(strategy = jakarta.persistence.InheritanceType.TABLE_PER_CLASS)
@jakarta.persistence.Table(name = QldbLedger.TABLE_NAME)
public class QldbLedger extends AWSResource {

  protected static final String TABLE_NAME = "awsqldbledger";
  public static final String RESOURCE_TYPE = "AWS::Qldb::Ledger";

  public QldbLedger() {
    this.resourceType = RESOURCE_TYPE;
  }

  public QldbLedger(String account, String regionId, DescribeLedgerResponse ledger) {
    this.awsRegion = regionId;
    this.awsAccountId = account;
    this.arn = ledger.arn();
    this.resourceName = ledger.name();
    this.configuration = PayloadUtils.update(ledger);
    this.resourceType = RESOURCE_TYPE;
    this.createdIso = ledger.creationDateTime();
  }
}
