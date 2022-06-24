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
package io.openraven.magpie.data.aws.ess;

import io.openraven.magpie.data.aws.AWSResource;
import io.openraven.magpie.data.aws.shared.PayloadUtils;
import software.amazon.awssdk.services.elasticsearch.model.ElasticsearchDomainStatus;

@javax.persistence.Entity
@javax.persistence.Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
@javax.persistence.Table(name = EssDomain.TABLE_NAME)
public class EssDomain extends AWSResource {

  protected static final String TABLE_NAME = "awselasticsearchdomain";
  public static final String RESOURCE_TYPE = "AWS::Elasticsearch::Domain";

  public EssDomain() {
    this.resourceType = RESOURCE_TYPE;
  }

  public EssDomain(String accountId, String regionId, ElasticsearchDomainStatus domainStatus) {
    this.arn = domainStatus.arn();
    this.awsAccountId = accountId;
    this.resourceId = domainStatus.domainId();
    this.resourceName = domainStatus.domainName();
    this.awsRegion = regionId;
    this.configuration = PayloadUtils.update(configuration, domainStatus);
    this.resourceType = RESOURCE_TYPE;
  }
}
