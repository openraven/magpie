package io.openraven.nightglow.core.fifos;

public class FifoException extends Exception {
  public FifoException() {
  }

  public FifoException(String message) {
    super(message);
  }

  public FifoException(String message, Throwable cause) {
    super(message, cause);
  }

  public FifoException(Throwable cause) {
    super(cause);
  }

  public FifoException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
