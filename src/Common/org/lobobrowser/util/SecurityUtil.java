package org.lobobrowser.util;

import java.security.AccessController;
import java.security.PrivilegedAction;

public class SecurityUtil {

  public static <T> T doPrivileged(final PrivilegedAction<T> action) {
    final SecurityManager sm = System.getSecurityManager();
    if (sm == null) {
      return action.run();
    } else {
      return AccessController.doPrivileged(action);
    }
  }
}
