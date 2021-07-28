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

package io.openraven.magpie.core.dmap;


import io.openraven.magpie.core.dmap.service.DMapLambdaServiceImpl;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Scanner;

public class Util {
  private Util() {}

  public static String getResourceAsString(String resourcePath) {
    return new Scanner(
      Objects.requireNonNull(DMapLambdaServiceImpl.class.getResourceAsStream(resourcePath)),
      StandardCharsets.UTF_8)
      .useDelimiter("\\A").next();
  }
}
