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
package io.openraven.magpie.data.gcp.project;

import io.openraven.magpie.data.gcp.GCPResource;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Table;

@Entity
@Inheritance(strategy = jakarta.persistence.InheritanceType.TABLE_PER_CLASS)
@Table(name = ProjectInfo.TABLE_NAME)
public class ProjectInfo extends GCPResource {

    protected static final String TABLE_NAME = "gcpprojectinfo";
    public static final String RESOURCE_TYPE = "GCP::Project::Info";

    public ProjectInfo() {
        this.resourceType = RESOURCE_TYPE;
    }
}
