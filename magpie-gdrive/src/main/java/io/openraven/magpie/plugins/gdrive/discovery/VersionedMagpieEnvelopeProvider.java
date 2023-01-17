package io.openraven.magpie.plugins.gdrive.discovery;

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
    metadata.put("magpie.gdrive.version", versionProvider.getProjectVersion());
    envelope.setMetadata(metadata);

    return envelope;
  }
}