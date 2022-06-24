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
package io.openraven.magpie.data.aws.athena;

import io.openraven.magpie.data.aws.AWSResource;
import io.openraven.magpie.data.aws.shared.PayloadUtils;
import software.amazon.awssdk.services.athena.model.DataCatalogSummary;

import static java.lang.String.format;

@javax.persistence.Entity
@javax.persistence.Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
@javax.persistence.Table(name = AthenaDataCatalog.TABLE_NAME)
public class AthenaDataCatalog extends AWSResource {

  protected static final String TABLE_NAME = "awsathenadatacatalog";
  public static final String RESOURCE_TYPE = "AWS::Athena::DataCatalog";

  public AthenaDataCatalog() {
    this.resourceType = RESOURCE_TYPE;

  }

  public AthenaDataCatalog(String account, String regionId, DataCatalogSummary dataCatalogSummary) {
    this.awsRegion = regionId;
    this.awsAccountId = account;
    this.arn = format("arn:aws:athena:%s:%s:datacatalog/%s", regionId, account, dataCatalogSummary.catalogName());
    this.resourceName = dataCatalogSummary.catalogName();
    this.configuration = PayloadUtils.update(dataCatalogSummary);
    this.resourceType = RESOURCE_TYPE;
  }
}
