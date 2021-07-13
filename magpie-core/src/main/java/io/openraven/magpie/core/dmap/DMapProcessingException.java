package io.openraven.magpie.core.dmap;

public class DMapProcessingException extends RuntimeException {

  public DMapProcessingException(String message) {
    super(message);
  }

  public DMapProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
