package org.lobobrowser.html.renderer;

class SizeExceededException extends RuntimeException {
  public SizeExceededException() {
    super();
  }

  public SizeExceededException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public SizeExceededException(final String message) {
    super(message);
  }

  public SizeExceededException(final Throwable cause) {
    super(cause);
  }
}
