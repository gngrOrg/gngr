package org.lobobrowser.html.renderer;

class SizeExceededException extends RuntimeException {
  private static final long serialVersionUID = 5789004695720876706L;

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
