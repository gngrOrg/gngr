package org.lobobrowser.security;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.lobobrowser.security.PermissionSystem.Permission;
import org.lobobrowser.security.PermissionSystem.PermissionBoard.PermissionRow;
import org.lobobrowser.ua.NavigationEntry;
import org.lobobrowser.ua.NavigatorFrame;
import org.lobobrowser.ua.RequestStats;
import org.lobobrowser.ua.RequestStatsListener;
import org.lobobrowser.ua.UserAgentContext;
import org.lobobrowser.ua.UserAgentContext.Request;
import org.lobobrowser.ua.UserAgentContext.RequestKind;
import org.lobobrowser.util.gui.GUITasks;

public final class RequestManager {
  private static final Logger logger = Logger.getLogger(RequestManager.class.getName());

  private final NavigatorFrame frame;


  public RequestManager(final NavigatorFrame frame) {
    this.frame = frame;
  }

  private static class RequestCounters {
    private final int counters[] = new int[UserAgentContext.RequestKind.values().length];

    void updateCounts(final RequestKind kind) {
      counters[kind.ordinal()]++;
    }

    @Override
    public String toString() {
      return Arrays.stream(RequestKind.values())
          .map(kind -> String.format(" %2d", counters[kind.ordinal()])).reduce((e, a) -> e + a)
          .orElse("");
    }
  }

  private Map<String, RequestCounters> hostToCounterMap = new HashMap<>();
  private Optional<PermissionSystem> permissionSystemOpt = Optional.empty();
  private ManageDialog activeDialog;
  private int allowedCount = 0;
  private int blockedCount = 0;
  private final CopyOnWriteArrayList<RequestStatsListener> statsListeners = new CopyOnWriteArrayList<>();

  public synchronized RequestStats getRequestStats() {
    return new RequestStats(blockedCount, allowedCount);
  }

  public void addStatsListener(final RequestStatsListener listener) {
    if (listener != null) {
      statsListeners.add(listener);
    }
  }

  public void removeStatsListener(final RequestStatsListener listener) {
    if (listener != null) {
      statsListeners.remove(listener);
    }
  }

  private synchronized RequestStats recordDecision(final boolean permitted) {
    if (permitted) {
      allowedCount++;
    } else {
      blockedCount++;
    }
    return new RequestStats(blockedCount, allowedCount);
  }

  private void notifyStatsListeners(final RequestStats stats) {
    for (final RequestStatsListener listener : statsListeners) {
      listener.requestStatsUpdated(stats);
    }
  }

  private synchronized void updateCounter(final Request request) {
    final String host = request.url.getHost().toLowerCase();
    updateCounter(host, request.kind);
  }

  private synchronized void updateCounter(final String host, final RequestKind kind) {
    ensureHostInCounter(host);
    hostToCounterMap.get(host).updateCounts(kind);
  }

  private void ensureHostInCounter(final String host) {
    if (!hostToCounterMap.containsKey(host)) {
      hostToCounterMap.put(host, new RequestCounters());
    }
  }

  private Optional<NavigationEntry> getFrameNavigationEntry() {
    final NavigationEntry currentNavigationEntry = frame.getCurrentNavigationEntry();
    return Optional.ofNullable(currentNavigationEntry);
  }

  private Optional<String> getFrameHost() {
    return getFrameNavigationEntry().map(e -> {
      final String host = e.getUrl().getHost();
      return host == null ? "" : host.toLowerCase();
    });
  }

  private Optional<URL> getFrameURL() {
    return getFrameNavigationEntry().map(e -> e.getUrl());
  }

  private Request rewriteRequest(final Request request) {
    final Optional<String> frameHostOpt = getFrameHost();
    if (request.url.getProtocol().equals("data") && frameHostOpt.isPresent()) {
      try {
        return new Request(new URL("data", frameHostOpt.get(), "someDataPath"), request.kind);
      } catch (final MalformedURLException e) {
        throw new RuntimeException("Couldn't rewrite data request");
      }
    } else {
      return request;
    }
  }

  public boolean isRequestPermitted(final Request request) {

    final Request finalRequest = rewriteRequest(request);

    if (permissionSystemOpt.isPresent()) {
      boolean permitted = permissionSystemOpt.map(p -> p.isRequestPermitted(finalRequest)).orElse(false);
      updateCounter(finalRequest);
      if (permitted) {
        final boolean httpPermitted = permissionSystemOpt.get().isUnsecuredHTTPPermitted(finalRequest);
        final String protocolTest = request.url.getProtocol();
        final String frameProtocol = getFrameURL().map(URL::getProtocol).orElse("");

        if ("http".equals(protocolTest) && "https".equals(frameProtocol)) {
          updateCounter(finalRequest.url.getHost(), RequestKind.UnsecuredHTTP);
          if (!httpPermitted) {
            permitted = false;
          }
        }
      }

      final RequestStats snapshot = recordDecision(permitted);
      notifyStatsListeners(snapshot);
      // dumpCounters();
      return permitted;
    } else {
      logger.severe("Unexpected permission system state. Request without context!");
      return false;
    }
  }

  private void setupPermissionSystem(final String frameHost) {
    final RequestRuleStore permissionStore = RequestRuleStore.getStore();
    final PermissionSystem system = new PermissionSystem(frameHost, permissionStore);

    // Prime the boards with atleast one row
    system.getLastBoard().getRow(frameHost);

    permissionSystemOpt = Optional.of(system);
  }

  @SuppressWarnings("unused")
  private synchronized void dumpCounters() {
    // Headers
    System.out.print(String.format("%30s  ", ""));
    getRequestKindNames().forEach(kindName -> System.out.print(" " + kindName.substring(0, 2)));
    System.out.println("");

    // Table rows
    hostToCounterMap.forEach((host, counters) -> {
      System.out.println(String.format("%30s: %s", "[" + host + "]", counters));
    });
  }

  private static Stream<String> getRequestKindNames() {
    return Arrays.stream(RequestKind.values()).map(kind -> kind.shortName);
  }

  public void reset(final URL frameUrl) {
    final ManageDialog dialogToClose;
    synchronized (this) {
      dialogToClose = activeDialog;
      activeDialog = null;
      hostToCounterMap = new HashMap<>();
      allowedCount = 0;
      blockedCount = 0;
      final String frameHostOrig = frameUrl.getHost();
      final String frameHost = frameHostOrig == null ? "" : frameHostOrig.toLowerCase();
      ensureHostInCounter(frameHost);
      setupPermissionSystem(frameHost);
    }
    if (dialogToClose != null) {
      if (SwingUtilities.isEventDispatchThread()) {
        dialogToClose.closeDialog(false);
      } else {
        SwingUtilities.invokeLater(() -> dialogToClose.closeDialog(false));
      }
    }
    notifyStatsListeners(RequestStats.EMPTY);
  }

  public void manageRequests(final JComponent initiatorComponent) {
    final ManageDialog existing;
    synchronized (this) {
      existing = activeDialog;
      activeDialog = null;
    }
    if (existing != null) {
      if (existing.isVisible()) {
        existing.closeDialog(true);
        return;
      } else if (existing.isDisplayable()) {
        existing.closeDialog(false);
      }
    }

    final PermissionSystem permissionSystem;
    final String[][] requestData;
    synchronized (this) {
      permissionSystem = permissionSystemOpt.orElse(null);
      if (permissionSystem == null) {
        return;
      }
      requestData = getRequestData();
    }

    final Window owner = initiatorComponent != null ? SwingUtilities.getWindowAncestor(initiatorComponent) : null;
    final ManageDialog dlg = new ManageDialog(owner, getFrameURL().map(URL::toExternalForm).orElse("Empty!"),
        initiatorComponent, permissionSystem, requestData);
    final boolean shouldShow;
    synchronized (this) {
      shouldShow = permissionSystemOpt.orElse(null) == permissionSystem;
      if (shouldShow) {
        activeDialog = dlg;
      }
    }
    if (shouldShow) {
      dlg.setVisible(true);
    } else {
      dlg.closeDialog(false);
    }
  }

  private synchronized String[][] getRequestData() {
    // hostToCounterMap.keySet().stream().forEach(System.out::println);

    return hostToCounterMap.entrySet().stream().map(entry -> {
      final List<String> rowElements = new LinkedList<>();
      rowElements.add(entry.getKey());
      Arrays.stream(entry.getValue().counters).forEach(c -> rowElements.add(Integer.toString(c)));

      return rowElements.toArray(new String[0]);
    }).toArray(String[][]::new);
  }

  private static String[] getColumnNames() {
    final List<String> kindNames = getRequestKindNames().collect(Collectors.toList());
    kindNames.add(0, "All");
    return kindNames.toArray(new String[0]);
  }

  public final class ManageDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = -2284357432219717106L;
    private final JComponent initiator;
    private final PermissionSystem dialogPermissionSystem;
    private final String initialPermissionStates;
    private final HierarchyBoundsListener hierarchyBoundsListener;
    private final AWTEventListener outsideClickListener;
    private boolean reloadOnClose = true;

    public ManageDialog(final Window parent, final String title, final JComponent initiator,
        final PermissionSystem permissionSystem, final String[][] requestData) {
      super(parent, title, ModalityType.MODELESS);
      this.initiator = initiator;
      this.dialogPermissionSystem = permissionSystem;
      this.initialPermissionStates = permissionSystem.getPermissionsAsString();
      setUndecorated(true);
      if (parent != null) {
        final Dimension parentSize = parent.getSize();
        final Point p = parent.getLocation();
        setLocation(p.x + (parentSize.width / 4), p.y + (parentSize.height / 4));
      }

      final JComponent table = PermissionTable.makeTable(permissionSystem, getColumnNames(), requestData);
      final JScrollPane scrollTablePane = new JScrollPane(table);

      getContentPane().add(scrollTablePane);

      final JPanel buttonPane = new JPanel();
      final JButton button = new JButton("OK");
      buttonPane.add(button);
      button.addActionListener(this);
      getContentPane().add(buttonPane, BorderLayout.SOUTH);
      setDefaultCloseOperation(DISPOSE_ON_CLOSE);
      addWindowListener(new WindowListenerImpl());

      pack();
      updateLocation();
      if (initiator != null) {
        this.hierarchyBoundsListener = new HierarchyBoundsListener() {

          @Override
          public void ancestorResized(final HierarchyEvent e) {
            updateLocation();
          }

          @Override
          public void ancestorMoved(final HierarchyEvent e) {
            updateLocation();
          }
        };
        initiator.addHierarchyBoundsListener(hierarchyBoundsListener);
      } else {
        this.hierarchyBoundsListener = null;
      }

      GUITasks.addEscapeListener(this);

      this.outsideClickListener = event -> {
        if (!isVisible() || !(event instanceof MouseEvent)) {
          return;
        }
        final MouseEvent mouseEvent = (MouseEvent) event;
        if (mouseEvent.getID() != MouseEvent.MOUSE_PRESSED) {
          return;
        }
        final Object source = mouseEvent.getSource();
        if (source instanceof Component) {
          final Component comp = (Component) source;
          if (SwingUtilities.isDescendingFrom(comp, ManageDialog.this)) {
            return;
          }
          if ((initiator != null) && SwingUtilities.isDescendingFrom(comp, initiator)) {
            return;
          }
        }
        closeDialog(true);
      };
      Toolkit.getDefaultToolkit().addAWTEventListener(this.outsideClickListener, AWTEvent.MOUSE_EVENT_MASK);
    }

    void closeDialog(final boolean allowReload) {
      if (!allowReload) {
        reloadOnClose = false;
      }
      setVisible(false);
      dispose();
    }

    public void actionPerformed(final ActionEvent e) {
      closeDialog(true);
    }

    @Override
    public void dispose() {
      Toolkit.getDefaultToolkit().removeAWTEventListener(outsideClickListener);
      if ((initiator != null) && (hierarchyBoundsListener != null)) {
        initiator.removeHierarchyBoundsListener(hierarchyBoundsListener);
      }
      synchronized (RequestManager.this) {
        if (activeDialog == this) {
          activeDialog = null;
        }
      }
      super.dispose();
    }

    private void updateLocation() {
      if ((initiator != null) && initiator.isShowing()) {
        final Point locationOnScreen = initiator.getLocationOnScreen();
        locationOnScreen.translate(initiator.getWidth() - getWidth(), initiator.getHeight());
        setLocation(locationOnScreen);
      }
    }

    private boolean isPointerInsideComponent(final Component component) {
      if ((component == null) || !component.isShowing()) {
        return false;
      }
      final PointerInfo pointerInfo = MouseInfo.getPointerInfo();
      if (pointerInfo == null) {
        return false;
      }
      final Point pointerLoc = pointerInfo.getLocation();
      final Point compLoc = component.getLocationOnScreen();
      return (pointerLoc.x >= compLoc.x)
          && (pointerLoc.x < (compLoc.x + component.getWidth()))
          && (pointerLoc.y >= compLoc.y)
          && (pointerLoc.y < (compLoc.y + component.getHeight()));
    }

    private final class WindowListenerImpl implements WindowListener {
      private boolean closeHandled = false;

      @Override
      public void windowOpened(final WindowEvent e) {
      }

      @Override
      public void windowIconified(final WindowEvent e) {
      }

      @Override
      public void windowDeiconified(final WindowEvent e) {
      }

      @Override
      public void windowDeactivated(final WindowEvent e) {
        if (!isVisible()) {
          return;
        }
        final Window opposite = e.getOppositeWindow();
        if ((opposite == ManageDialog.this) || (opposite == getOwner())) {
          return;
        }
        if ((opposite == null) && isPointerInsideComponent(initiator)) {
          return;
        }
        closeDialog(true);
      }

      @Override
      public void windowClosing(final WindowEvent e) {
      }

      @Override
      public void windowClosed(final WindowEvent e) {
        if (closeHandled || !reloadOnClose) {
          return;
        }
        closeHandled = true;
        final boolean matchesCurrentSystem;
        synchronized (RequestManager.this) {
          matchesCurrentSystem = permissionSystemOpt.orElse(null) == dialogPermissionSystem;
        }
        if (matchesCurrentSystem) {
          final String finalPermissionStates = dialogPermissionSystem.getPermissionsAsString();
          if (!finalPermissionStates.equals(initialPermissionStates)) {
            frame.reload();
          }
        }
      }

      @Override
      public void windowActivated(final WindowEvent e) {
      }
    }

  }

  public void allowAllFirstPartyRequests() {
    permissionSystemOpt.ifPresent(p -> {
      final PermissionRow row = p.getLastBoard().getRow(getFrameHost().get());
      row.getHostCell().setPermission(Permission.Allow);
    });
  }

}
