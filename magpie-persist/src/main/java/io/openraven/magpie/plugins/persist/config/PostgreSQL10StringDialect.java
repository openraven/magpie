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

package io.openraven.magpie.plugins.persist.config;

import org.hibernate.dialect.PostgreSQL10Dialect;
import org.hibernate.type.StringType;

import java.sql.Types;

public class PostgreSQL10StringDialect extends PostgreSQL10Dialect {

  public PostgreSQL10StringDialect() {
    super();
    this.registerHibernateType(
      Types.OTHER, StringType.class.getName()
    );
  }
}
