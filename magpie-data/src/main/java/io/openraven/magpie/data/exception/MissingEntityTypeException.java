package io.openraven.magpie.data.exception;

import java.io.IOException;

/**
 * Signals that an I/O exception occurred due to an asset type being consumed that isn't supported by the library.
 */
public class MissingEntityTypeException extends IOException {

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
