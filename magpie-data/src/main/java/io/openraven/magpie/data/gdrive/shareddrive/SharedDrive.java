/*-
 * #%L
 * Magpie API
 * %%
 * Copyright (C) 2021 - 2023 Open Raven Inc
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
package io.openraven.magpie.data.gdrive.shareddrive;

import io.openraven.magpie.data.gdrive.GDriveResource;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.Table;

@Entity
@Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
@Table(name = io.openraven.magpie.data.gdrive.shareddrive.SharedDrive.TABLE_NAME)
public class SharedDrive extends GDriveResource {

  protected static final String TABLE_NAME = "gdriveshareddrive";
  public static final String RESOURCE_TYPE = "GDrive::SharedDrive::SharedDrive";

  public SharedDrive() {
    this.resourceType = RESOURCE_TYPE;
  }
}
