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

import io.openraven.magpie.data.Resource;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

public interface AssetsRepo extends Closeable {

  void upsert(Resource awsResource);

  void upsert(List<Resource> awsResources);

  void executeNative(String query);

  List<Map<String, Object>> queryNative(String query);

  Long getAssetCount(String resourceType);
}
