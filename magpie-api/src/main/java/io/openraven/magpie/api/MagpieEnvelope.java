/*-
 * #%L
 * magpie-api
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

package io.openraven.magpie.api;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Jackson-serializable envelope that is passed from layer to layer via plugin emitters and acceptors.
 * @author Jason Nichols (jason@openraven.com)
 */
public class MagpieEnvelope {

  /**
   * {@link IntermediatePlugin} implementations should use this static factory method to create a new envelope that derives
   * from an existing envelope.  This ensures the continuity of the envelops pluginPath.
   * @param current The currently received envelope that the plugin is processing.
   * @param pluginId The ID of the current plugin
   * @param contents The contents (preferably JSON) to be passed downstream to the next layer.
   * @return The newly derived envelope.
   */
  public static MagpieEnvelope of(MagpieEnvelope current, String pluginId, ObjectNode contents) {
    final List<String> newPath = new ArrayList<>(current.pluginPath);
    newPath.add(pluginId);
    final var env = new MagpieEnvelope(current.getSession(), newPath, contents);
    env.setMetadata(new HashMap<>(current.getMetadata()));
    return env;
  }

  private Session session;
  private List<String> pluginPath;
  private Map<String, String> metadata = new HashMap<>();
  private ObjectNode contents;
  private String contentClass;


  public MagpieEnvelope() {
  }

  public MagpieEnvelope(Session session, List<String> pluginPath, ObjectNode contents) {
    this.session = session;
    this.pluginPath = pluginPath;
    this.contents = contents;
    this.contentClass = contents.getClass().getName();
  }

  public Session getSession() {
    return session;
  }

  public void setSession(Session session) {
    this.session = session;
  }

  /**
   * The pluginPath list represents this envelope's path through the Magpie framework. Each plugin that operates
   * on data should append it's ID to the ordered list. This allows downstream plugins to make decisions based on
   * expected schemas for upstream plugins or sub-services.
   * @return
   */
  public List<String> getPluginPath() {
    return pluginPath;
  }

  public void setPluginPath(List<String> pluginPath) {
    this.pluginPath = pluginPath;
  }

  public ObjectNode getContents() {
    return contents;
  }

  public void setContents(ObjectNode contents) {
    this.contents = contents;
  }

  public String getContentClass() {
    return contentClass;
  }

  public void setContentClass(String contentClass) {
    this.contentClass = contentClass;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }
}
