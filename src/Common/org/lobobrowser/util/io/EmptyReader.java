package org.lobobrowser.util.io;

import java.io.IOException;
import java.io.Reader;

public class EmptyReader extends Reader {
  @Override
  public void close() throws IOException {
  }

  @Override
  public int read(final char[] cbuf, final int off, final int len) throws IOException {
    return 0;
  }
}
