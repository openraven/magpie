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

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openraven.magpie.api.MagpieEnvelope;
import io.openraven.magpie.api.Session;

import java.util.List;
import java.util.Map;

public class VersionedMagpieEnvelopeProvider {
  private static final VersionProvider versionProvider = new VersionProvider();

  public static MagpieEnvelope create(Session session, List<String> pluginPath, ObjectNode contents) {
    var envelope = new MagpieEnvelope(session, pluginPath, contents);

    Map<String, String> metadata = envelope.getMetadata();
    metadata.put("magpie.aws.version", versionProvider.getProjectVersion());
    metadata.put("aws.sdk.version", versionProvider.getAwsSdkVersion());
    envelope.setMetadata(metadata);

    return envelope;
  }
}
