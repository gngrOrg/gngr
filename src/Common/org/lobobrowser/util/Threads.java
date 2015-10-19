/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2014 Uproot Labs India Pvt Ltd

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

 */
package org.lobobrowser.util;

public final class Threads {

  final private static int STACKS_TO_SKIP_AT_START = 2;

  public static void dumpStack(final int maxStacks) {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final int stacksToPrint = Math.min(stackTrace.length, maxStacks + STACKS_TO_SKIP_AT_START);
    System.out.println("--- 8< ------------[START]------------ >8 ---");
    for (int i = STACKS_TO_SKIP_AT_START; i < stacksToPrint; i++) {
      System.out.println(stackTrace[i]);
    }
    if (stacksToPrint < stackTrace.length) {
      System.out.println("... skipped " + (stackTrace.length - stacksToPrint) + " traces");
    }
    System.out.println("--- 8< ------------[ END ]------------ >8 ---");
  }

  /** Sleep until interrupted */
  public static void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
