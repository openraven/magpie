/*-
 * #%L
 * Magpie API
 * %%
 * Copyright (C) 2021 - 2022 Open Raven Inc
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
package io.openraven.magpie.data.exception;


/**
 * Signals that an exception occurred due to an asset type being consumed that isn't supported by the library.
 */
public class MissingEntityTypeException extends RuntimeException {

  /**
   * Constructs an {@code MissingEntityTypeException} with the specified detail message.
   *
   * @param message
   *        The detail message (which is saved for later retrieval
   *        by the {@link #getMessage()} method)
   */
  public MissingEntityTypeException(String message) {
    super(message);
  }

}
