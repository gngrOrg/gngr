package org.lobobrowser.util;

import org.w3c.dom.DOMException;

public final class DOMExceptions {
  public static enum ExtendedError {
    SecurityError((short) 18),
    NetworkError((short) 19),
    AbortError((short) 20),
    URLMismatchError((short) 21),
    QuotaExceededError((short) 22),
    TimeoutError((short) 23),
    InvalidNodeTypeError((short) 24),
    DataCloneError((short) 25);

    final public short code;

    public DOMException createException() {
      return new DOMException(code, name());
    }
    
    public DOMException createException(final String msg) {
      return new DOMException(code, name() + ": " + msg);
    }

    private ExtendedError (final short code) {
      this.code = code;
    }

  }
}
