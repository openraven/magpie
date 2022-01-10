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


public class PersistConfig {
  private String hostname;
  private String port;
  private String databaseName;
  private String user;
  private String password;
  private boolean migrateDB = true;

  public String getHostname() { return hostname;}
  public void setHostname(String hostname) {
    this.hostname = hostname == null ? "" : hostname;
  }

  public String getPort() { return port;}
  public void setPort(String port) {
    this.port = port == null ? "" : port;
  }

  public String getDatabaseName() { return databaseName;}
  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName == null ? "" : databaseName;
  }

  public String getUser() { return user;}
  public void setUser(String user) {
    this.user = user == null ? "" : user;
  }

  public String getPassword() { return password;}
  public void setPassword(String password) {
    this.password = password == null ? "" : password;
  }

    public boolean shouldMigrateDB() {
        return migrateDB;
    }

    public void setMigrateDB(boolean migrateDB) {
        this.migrateDB = migrateDB;
    }
}
