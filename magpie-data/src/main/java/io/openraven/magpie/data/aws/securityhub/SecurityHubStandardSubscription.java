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
package io.openraven.magpie.data.aws.securityhub;

import io.openraven.magpie.data.aws.AWSResource;

@jakarta.persistence.Entity
@jakarta.persistence.Inheritance(strategy = jakarta.persistence.InheritanceType.TABLE_PER_CLASS)
@jakarta.persistence.Table(name = SecurityHubStandardSubscription.TABLE_NAME)
public class SecurityHubStandardSubscription extends AWSResource {

  protected static final String TABLE_NAME = "awssecurityhubstandardsubscription";
  public static final String RESOURCE_TYPE = "AWS::SecurityHub::StandardsSubscription";

  public SecurityHubStandardSubscription() {
    this.resourceType = RESOURCE_TYPE;
  }

}
