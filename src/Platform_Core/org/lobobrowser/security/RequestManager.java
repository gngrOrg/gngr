package org.lobobrowser.security;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.lobobrowser.security.PermissionSystem.Permission;
import org.lobobrowser.security.PermissionSystem.PermissionBoard.PermissionRow;
import org.lobobrowser.ua.NavigationEntry;
import org.lobobrowser.ua.NavigatorFrame;
import org.lobobrowser.ua.UserAgentContext;
import org.lobobrowser.ua.UserAgentContext.Request;
import org.lobobrowser.ua.UserAgentContext.RequestKind;
import org.lobobrowser.util.gui.GUITasks;

public final class RequestManager {
  private static final Logger logger = Logger.getLogger(RequestManager.class.getName());

  private final NavigatorFrame frame;

  /**
   * Constructor for the RequestManager class
   * 
   * @param frame
   *          navigation frame that the user will interact with
   */
  public RequestManager(final NavigatorFrame frame) {
    this.frame = frame;
  }

  /**
   * RequestCounters: keeps track of all requests that were made to a given URL.
   * These can be associated with one of the following types
   * {@link org.lobobrowser.ua.UserAgentContext.RequestKind}: img CSS Cookie JS
   * Frame XHR Referrer Unsecured HTTP
   *
   * The values will be populated in the RequestManager GUI frame.
   */
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

  private int acceptedRequests = 0;
  private int rejectedRequests = 0;
  private Map<String, RequestCounters> hostToCounterMap = new HashMap<>();
  private Optional<PermissionSystem> permissionSystemOpt = Optional.empty();

  /**
   * updateCounter: helper function to update the counter for a given request
   * within the hostToCounterMap
   * 
   * @param request
   *          that has been made by the user
   */
  private synchronized void updateCounter(final Request request) {
    final String host = request.url.getHost().toLowerCase();
    updateCounter(host, request.kind);
  }

  /**
   * updateCounter: updates the counter for a given host based on the kind of
   * request that was queried
   * 
   * @param String
   *          specified host from URL bar
   * @param {@link
   *          org.lobobrowser.ua.UserAgentContext.RequestKind}
   */
  private synchronized void updateCounter(final String host, final RequestKind kind) {
    ensureHostInCounter(host);
    hostToCounterMap.get(host).updateCounts(kind);
  }

  /**
   * ensureHostInCounter: ensures that the given URL has been stored with it's
   * given request counters for it's returned types
   * 
   * @param String
   *          host from the URL bar
   */
  private void ensureHostInCounter(final String host) {
    if (!hostToCounterMap.containsKey(host)) {
      hostToCounterMap.put(host, new RequestCounters());
    }
  }

  /**
   * getNavigationEntry: gets a URL associated with a returned request for a
   * given page.
   * 
   * @return NavigationEntry navigation entry for the frames history
   */
  private Optional<NavigationEntry> getFrameNavigationEntry() {
    final NavigationEntry currentNavigationEntry = frame.getCurrentNavigationEntry();
    return Optional.ofNullable(currentNavigationEntry);
  }

  /**
   * getFrameHost: returns a specified hostname
   * 
   * @return String the requested hostname
   */
  private Optional<String> getFrameHost() {
    return getFrameNavigationEntry().map(e -> {
      final String host = e.getUrl().getHost();
      return host == null ? "" : host.toLowerCase();
    });
  }

  /**
   * getFrameURL: returns the frames current URL
   * 
   * @return String URL for the given frame
   */
  private Optional<URL> getFrameURL() {
    return getFrameNavigationEntry().map(e -> e.getUrl());
  }

  /**
   * rewriteRequest: will rewrite any request that has been made by a user
   * 
   * @param request
   *          Request object that was originally intended to be made
   * @return newly created request object
   */
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

  /**
   * isRequestPermitted: determines if a request is rejected or permitted by: 1)
   * Checking against the permission system to make sure the request is
   * permitted 2) If request is permitted update associated fields with request
   * parameters 3) Else update request has been rejected
   * 
   * @param request
   * @return boolean request has either been permitted or rejected
   */
  public boolean isRequestPermitted(final Request request) {

    final Request finalRequest = rewriteRequest(request);

    if (permissionSystemOpt.isPresent()) {
      final Boolean permitted = permissionSystemOpt.map(p -> p.isRequestPermitted(finalRequest)).orElse(false);
      updateCounter(finalRequest);
      if (permitted) {
        acceptedRequests++;
        final boolean httpPermitted = permissionSystemOpt.get().isUnsecuredHTTPPermitted(finalRequest);
        String protocolTest = request.url.getProtocol();
        String frameProtocol = getFrameURL().get().getProtocol();

        if (protocolTest.equals("http") && frameProtocol.equals("https")) {
          updateCounter(finalRequest.url.getHost(), RequestKind.UnsecuredHTTP);
          if (!httpPermitted) {
            return false;
          }
        }
      } else {
        rejectedRequests++;
      }

      // dumpCounters();
      return permitted;
    } else {
      logger.severe("Unexpected permission system state. Request without context!");
      return false;
    }
  }

  /**
   * setupPermissionSystem: inits and establishes the new permission system for
   * the RequestManager
   */
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

  /**
   * getRequestKindNames: returns a list of the types of requests that can be
   * made {@link org.lobobrowser.ua.UserAgentContext.RequestKind}
   * 
   * @return Stream<String> stream of shortened name for the given request
   */
  private static Stream<String> getRequestKindNames() {
    return Arrays.stream(RequestKind.values()).map(kind -> kind.shortName);
  }

  /**
   * reset: Resets all request information each time a new URL name is entered.
   * 
   * @param URL
   *          the current URL entered by the user
   */
  public synchronized void reset(final URL frameUrl) {
    hostToCounterMap = new HashMap<>();
    acceptedRequests = 0;
    rejectedRequests = 0;
    final String frameHostOrig = frameUrl.getHost();
    final String frameHost = frameHostOrig == null ? "" : frameHostOrig.toLowerCase();
    ensureHostInCounter(frameHost);
    setupPermissionSystem(frameHost);
  }

  /**
   * manageRequests: sets up the RequestManager component this is called from
   * {@link org.lobobrowser.gui.FramePanel}
   * 
   * @param initiatorComponent
   *          the top-level swing container
   * @return void
   */
  public void manageRequests(final JComponent initiatorComponent) {
    // permissionSystemOpt.ifPresent(r -> r.dump());
    final ManageDialog dlg = new ManageDialog(new JFrame(), getFrameURL().map(u -> u.toExternalForm()).orElse("Empty!"),
        initiatorComponent);
    dlg.setVisible(true);
  }

  /**
   * getRequestData: called by {@link org.lobobrowser.security.RequestManager}
   * and populates the RequestManager GUI with a requests return counter values.
   * 
   * @return String[][] a two dimensional array of a requests counter values
   */
  private synchronized String[][] getRequestData() {
    // hostToCounterMap.keySet().stream().forEach(System.out::println);

    return hostToCounterMap.entrySet().stream().map(entry -> {
      final List<String> rowElements = new LinkedList<>();
      rowElements.add(entry.getKey());
      Arrays.stream(entry.getValue().counters).forEach(c -> rowElements.add(Integer.toString(c)));

      return rowElements.toArray(new String[0]);
    }).toArray(String[][]::new);
  }

  /**
   * getAcceptRejectData: gets all of the associated accepted and rejected
   * requests values to be serialized and passed for GUI display
   * 
   * @return int[] an integer array sub two values 0 for accept 1 for reject
   */
  private synchronized int[] getAcceptRejectData() {
    int[] temp = new int[2];
    temp[0] = acceptedRequests;
    temp[1] = rejectedRequests;
    return temp;
  }

  /**
   * getColumnNames: populates the JComponent with the types if request return
   * types such as: img CSS Cookie JS Frame XHR Referrer Unsecured HTTP
   * {@link org.lobobrowser.ua.UserAgentContext.RequestKind}
   * 
   * @return String[] list of request type names
   */
  private static String[] getColumnNames() {
    final List<String> kindNames = getRequestKindNames().collect(Collectors.toList());
    kindNames.add(0, "All");
    return kindNames.toArray(new String[0]);
  }

  /**
   * ManageDialog Class: newly instantiated Dialog class that will hold the
   * elements for any requests that have been made and are to be managed by the
   * request manager.
   * 
   */
  public final class ManageDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = -2284357432219717106L;
    private final JComponent initiator;

    public ManageDialog(final JFrame parent, final String title, final JComponent initiator) {
      super(parent, title, true);
      this.initiator = initiator;
      setUndecorated(true);
      if (parent != null) {
        final Dimension parentSize = parent.getSize();
        final Point p = parent.getLocation();
        setLocation(p.x + (parentSize.width / 4), p.y + (parentSize.height / 4));
      }

      final JComponent table = PermissionTable.makeTable(permissionSystemOpt.get(), getColumnNames(), getRequestData(),
          getAcceptRejectData());
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
      initiator.addHierarchyBoundsListener(new HierarchyBoundsListener() {

        @Override
        public void ancestorResized(final HierarchyEvent e) {
          updateLocation();
        }

        @Override
        public void ancestorMoved(final HierarchyEvent e) {
          updateLocation();
        }
      });

      GUITasks.addEscapeListener(this);
    }

    public void actionPerformed(final ActionEvent e) {
      setVisible(false);
      dispose();
    }

    private void updateLocation() {
      final Point locationOnScreen = initiator.getLocationOnScreen();
      locationOnScreen.translate(initiator.getWidth() - getWidth(), initiator.getHeight());
      setLocation(locationOnScreen);
    }

    private final class WindowListenerImpl implements WindowListener {
      private String initialPermissionStates;

      @Override
      public void windowOpened(final WindowEvent e) {
        initialPermissionStates = permissionSystemOpt.get().getPermissionsAsString();
      }

      @Override
      public void windowIconified(final WindowEvent e) {
      }

      @Override
      public void windowDeiconified(final WindowEvent e) {
      }

      @Override
      public void windowDeactivated(final WindowEvent e) {
      }

      @Override
      public void windowClosing(final WindowEvent e) {
      }

      @Override
      public void windowClosed(final WindowEvent e) {
        final String finalPermissionStates = permissionSystemOpt.get().getPermissionsAsString();
        if (!finalPermissionStates.equals(initialPermissionStates)) {
          frame.reload();
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
