package org.lobobrowser.ua;

import java.util.EventListener;

/**
 * Listener notified when request permission statistics change for a frame.
 */
@FunctionalInterface
public interface RequestStatsListener extends EventListener {
  void requestStatsUpdated(RequestStats stats);
}
