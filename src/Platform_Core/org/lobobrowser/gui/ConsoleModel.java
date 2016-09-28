/*
    GNU GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The Lobo Project

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public
    License as published by the Free Software Foundation; either
    verion 2 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Contact info: lobochief@users.sourceforge.net
 */
/*
 * Created on Jun 18, 2005
 */
package org.lobobrowser.gui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.text.PlainDocument;
import javax.swing.text.Position;

/**
 * The Swing document model that is used to store console output.
 */
public class ConsoleModel extends PlainDocument {
  private static final long serialVersionUID = 1067196363975764005L;
  private static final int MAX_LENGTH = 20000;
  private static final Logger logger = Logger.getLogger(ConsoleModel.class.getName());
  private final OutputStream outputStream;

  /**
   *
   */
  public ConsoleModel() {
    super();
    this.outputStream = new LocalOutputStream();
  }

  private static ConsoleModel standard = new ConsoleModel();

  public static ConsoleModel getStandard() {
    return standard;
  }

  private void append(final byte[] bytes, final int offset, final int length) throws IOException {
    try {
      final String text = new String(bytes, offset, length, "ISO-8859-1");
      final Position endPosition = getEndPosition();
      insertString(endPosition.getOffset(), text, null);
      final int overflow = getLength() - MAX_LENGTH;
      if (overflow > 0) {
        final Position startPosition = getStartPosition();
        remove(startPosition.getOffset(), overflow);
      }
    } catch (final Exception err) {
      // No standard I/O should be here!
      logger.log(Level.SEVERE, "append()", err);
    }
  }

  public PrintStream getPrintStream() {
    return new PrintStream(this.outputStream);
  }

  private class LocalOutputStream extends OutputStream implements Runnable {
    private final LinkedList<byte[]> dataQueue = new LinkedList<>();

    public LocalOutputStream() {
      final Thread t = new Thread(this, "ConsoleOutputStream");
      t.setDaemon(true);
      t.start();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.io.Flushable#flush()
     */
    @Override
    public void flush() throws IOException {
    }

    /*
     * (non-Javadoc)
     *
     * @see java.io.OutputStream#write(byte[])
     */
    @Override
    public void write(final byte[] b) throws IOException {
      this.write(b, 0, b.length);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
      synchronized (this.dataQueue) {
        byte[] actualBytes;
        if ((off == 0) && (len == b.length)) {
          actualBytes = b;
        } else {
          actualBytes = new byte[len];
          System.arraycopy(b, off, actualBytes, 0, len);
        }
        this.dataQueue.add(actualBytes);
        this.dataQueue.notify();
      }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.io.OutputStream#write(int)
     */
    @Override
    public void write(final int b) throws IOException {
      this.write(new byte[] { (byte) b }, 0, 1);
    }

    public void run() {
      final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      for (;;) {
        try {
          buffer.reset();
          synchronized (this.dataQueue) {
            while (this.dataQueue.size() == 0) {
              this.dataQueue.wait();
            }
            final Iterator<byte[]> i = this.dataQueue.iterator();
            while (i.hasNext()) {
              final byte[] data = i.next();
              buffer.write(data);
            }
            this.dataQueue.clear();
          }
          final byte[] allNewBytes = buffer.toByteArray();
          append(allNewBytes, 0, allNewBytes.length);
          // Sleep a little so document model is not
          // constantly firing events.
          Thread.sleep(300);
        } catch (final Exception t) {
          t.printStackTrace(System.err);
        }
      }
    }
  }
}
