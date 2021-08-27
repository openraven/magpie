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

package io.openraven.magpie.plugins.aws.discovery;

import io.openraven.magpie.plugins.aws.discovery.exception.AwsDiscoveryException;

import java.io.IOException;
import java.util.Properties;

public class VersionProvider {
  private static final String RESOURCE = "version.properties";

  private final String awsSdkVersion;
  private final String projectVersion;

  public VersionProvider() {

    Properties properties = new Properties();
    try {
      properties.load(getClass().getClassLoader().getResourceAsStream(RESOURCE));
    } catch (IOException e) {
      throw new AwsDiscoveryException("Unable to parse properties from version.properties resource", e);
    }

    awsSdkVersion = properties.getProperty("aws.sdk.version");
    projectVersion = properties.getProperty("project.version");
  }

  public String getAwsSdkVersion() {
    return awsSdkVersion;
  }

  public String getProjectVersion() {
    return projectVersion;
  }
}
