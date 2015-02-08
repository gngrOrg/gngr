package org.lobobrowser.util;

/** Thrown when something hasn't been implemented yet.
 *  Based on an idea discussed in http://stackoverflow.com/questions/2329358/is-there-anything-like-nets-notimplementedexception-in-java
 *  */
public class NotImplementedYetException extends RuntimeException {

  public NotImplementedYetException() {
    super("Not Implemented Yet");
  }

  public NotImplementedYetException(final String msg) {
    super("Not Implemented Yet: " + msg);
  }

}
