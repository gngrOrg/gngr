package org.lobobrowser.security;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.javatuples.Pair;
import org.lobobrowser.ua.UserAgentContext.Request;
import org.lobobrowser.ua.UserAgentContext.RequestKind;

public class PermissionSystem {
	
  static enum Permission {
    Allow, Deny, Undecided;

    public boolean isDecided() {
      return this != Undecided;
    }
  }

  static final class PermissionResult {
    final Permission permission;
    final boolean isDefault;

    public PermissionResult(final Permission permission, final boolean isDefault) {
      this.permission = permission;
      this.isDefault = isDefault;
    }

    public PermissionResult makeDefault() {
      return new PermissionResult(permission, true);
    }

    @Override
    public String toString() {
      return permission.name() + (isDefault ? "" : "*");
    }
  }

  // private static Permission DefaultFallbackPermisson = Permission.Undecided;
  // private static PermissionResult DefaultFallbackPermissonResult = new PermissionResult(DefaultFallbackPermisson, true);

  private final List<PermissionBoard> boards = new LinkedList<>();
  private final RequestRuleStore store;
  final String frameHost;

  public PermissionSystem(final String frameHost, final RequestRuleStore store) {
    this.frameHost = frameHost;
    this.store = store;

    final PermissionBoard defaultBoard = new PermissionBoard("*", Optional.empty());

    final PermissionBoard frameBoard = new PermissionBoard("*." + frameHost, Optional.of(defaultBoard));
    boards.add(defaultBoard);
    boards.add(frameBoard);
  }

  public boolean isRequestPermitted(final Request request) {
    final String protocol = request.url.getProtocol();
    if ("http".equals(protocol) || "https".equals(protocol) || "data".equals(protocol)) {
      return getLastBoard().isRequestPermitted(request);
    } else if ("about".equals(protocol)) {
      return request.url.toString().equals("about:blank");
    } else {
      // Constrain all other protocols. Especially worrying is the file:// protocol
      // ... for issue #18
      return false;
    }
  }

  public boolean isUnsecuredHTTPPermitted(final Request request) {
    return getLastBoard().isUnsecuredHTTPPermitted(request);
  }

  public List<PermissionBoard> getBoards() {
    return boards;
  }

  public PermissionBoard getLastBoard() {
    return boards.get(boards.size() - 1);
  }

  public void dump() {
    boards.get(0).dump();
    getLastBoard().dump();
  }

  public String getPermissionsAsString() {
    StringBuilder permissions = new StringBuilder();
    for (PermissionBoard board : boards) {
      permissions.append(board.getBoardPermissionsAsString());
    }
    return permissions.toString();
  }

  static <T> Stream<T> streamOpt(final Optional<T> opt) {
    if (opt.isPresent()) {
      return Stream.of(opt.get());
    } else {
      return Stream.empty();
    }
  }

  public class PermissionBoard {
    private final Map<String, PermissionRow> requestHostMap = new HashMap<>();
    private final Optional<PermissionRow> headerRowOpt;
    private final Optional<PermissionBoard> fallbackBoardOpt;
    final String hostPattern;

    public PermissionBoard(final String hostPattern, final Optional<PermissionBoard> fallbackBoardOpt) {
      final boolean gpCellCanBeUndecidable = fallbackBoardOpt.isPresent();
      final Pair<Permission, Permission[]> headerPermissions = store.getPermissions(hostPattern, "");
      final PermissionRow headerRow = new PermissionRow("", headerPermissions, Optional.empty(), fallbackBoardOpt.map(b -> b.headerRowOpt
          .get()), gpCellCanBeUndecidable);
      this.headerRowOpt = Optional.of(headerRow);
      this.fallbackBoardOpt = fallbackBoardOpt;
      this.hostPattern = hostPattern;
    }

    public PermissionRow getRow(final String requestHost) {
      final PermissionRow row = requestHostMap.get(requestHost);
      if (row == null) {
        final Optional<PermissionRow> fallbackRow = fallbackBoardOpt.map(b -> b.getRow(requestHost));
        // final PermissionRow newRow = new PermissionRow(Permission.Undecided, getEmptyPermissions(), headerRowOpt, fallbackRow);
        final Pair<Permission, Permission[]> permissions = store.getPermissions(hostPattern, requestHost);
        final PermissionRow newRow = new PermissionRow(requestHost, permissions, headerRowOpt, fallbackRow, true);
        requestHostMap.put(requestHost, newRow);
        return newRow;
      } else {
        return row;
      }
    }

    public boolean isRequestPermitted(final Request request) {
      final String requestHost = request.url.getHost().toLowerCase();
      final PermissionRow row = getRow(requestHost);
      return row.isRequestPermitted(request);
    }

    public boolean isUnsecuredHTTPPermitted(final Request request) {
      final String requestHost = request.url.getHost().toLowerCase();
      final PermissionRow row = getRow(requestHost);
      return row.isUnsecuredHTTPPermitted();
    }

    public void dump() {
      System.out.print(String.format("\n%30s", "all"));
      headerRowOpt.ifPresent(r -> r.dump());
      System.out.println("\n                          ---------------------------------");
      requestHostMap.forEach((host, row) -> {
        System.out.print(String.format("%30s", host));
        row.dump();
        System.out.println("");
      });
    }

    public String getBoardPermissionsAsString() {
      StringBuilder permissionState = new StringBuilder();
      permissionState.append(headerRowOpt.isPresent() ? headerRowOpt.get().getRowPermissionsAsString() : "");
      requestHostMap.forEach((host, row) -> {
        permissionState.append(row.getRowPermissionsAsString());
      });
      return permissionState.toString();
    }

    public int getRowCount() {
      return requestHostMap.size();
    }

    public List<Entry<String, PermissionRow>> getRows() {
      return requestHostMap.entrySet().stream().collect(Collectors.toList());
    }

    public PermissionRow getHeaderRow() {
      return headerRowOpt.get();
    }

    public class PermissionRow {
      private final PermissionCell hostCell;
      private final PermissionCell[] requestCells = new PermissionCell[RequestKind.numKinds()];
      private final String requestHost;

      public PermissionRow(final String requestHost, final Pair<Permission, Permission[]> initialRequestPermissions,
          final Optional<PermissionRow> headerRowOpt,
          final Optional<PermissionRow> fallbackRowOpt, final boolean hostCanBeUndecidable) {
        this.requestHost = requestHost;
        final List<PermissionCell> hostParentList = streamOpt(headerRowOpt.map(r -> r.hostCell)).collect(Collectors.toList());
        final Permission hostPermission = initialRequestPermissions.getValue0();
        final Permission[] kindPermissions = initialRequestPermissions.getValue1();
        hostCell = new PermissionCell(Optional.empty(), hostPermission, hostParentList, fallbackRowOpt.map(r -> r.hostCell),
            Optional.empty(), hostCanBeUndecidable);
        IntStream.range(0, requestCells.length).forEach(i -> {
          final LinkedList<PermissionCell> parentCells = makeParentCells(headerRowOpt, i);
          final Optional<PermissionCell> grandParentCellOpt = headerRowOpt.map(r -> r.hostCell);
          final Optional<PermissionCell> fallbackCellOpt = fallbackRowOpt.map(r -> r.requestCells[i]);
          final Optional<RequestKind> kindOpt = Optional.of(RequestKind.forOrdinal(i));
          requestCells[i] = new PermissionCell(kindOpt, kindPermissions[i], parentCells, fallbackCellOpt, grandParentCellOpt, true);
        });
      }

      private LinkedList<PermissionCell> makeParentCells(final Optional<PermissionRow> headerRowOpt, final int i) {
        final LinkedList<PermissionCell> parentCells = new LinkedList<>();
        parentCells.add(hostCell);
        headerRowOpt.ifPresent(headerRow -> {
          parentCells.add(headerRow.requestCells[i]);
        });
        return parentCells;
      }

      public boolean isRequestPermitted(final Request request) {
        return getPermission(request).permission == Permission.Allow;
      }

      public PermissionResult getPermission(final Request request) {
        final int requestOrdinal = request.kind.ordinal();
        return requestCells[requestOrdinal].getEffectivePermission();
      }

      public boolean isUnsecuredHTTPPermitted() {
        return requestCells[RequestKind.UnsecuredHTTP.ordinal()].getEffectivePermission().permission == Permission.Allow;
      }

      public void dump() {
        Arrays.stream(requestCells).forEach(c -> {
          final PermissionResult permissionResult = c.getEffectivePermission();
          final String permStr = permissionResult.permission.toString().substring(0, 1);
          final String permFmtStr = permissionResult.isDefault ? " " + permStr + " " : "[" + permStr + "]";
          System.out.print(String.format(" %s%d", permFmtStr, c.parentCells.size()));
        });
      }

      public String getRowPermissionsAsString() {
        StringBuilder rowPermissionState = new StringBuilder();
        Arrays.stream(requestCells).forEach(c -> {
          final PermissionResult permissionResult = c.getEffectivePermission();
          final String permStr = permissionResult.permission.toString().substring(0, 1);
          final String permFmtStr = permissionResult.isDefault ? " " + permStr + " " : "[" + permStr + "]";
          rowPermissionState.append(permFmtStr);
        });
        return rowPermissionState.toString();
      }

      public PermissionCell getHostCell() {
        return hostCell;
      }

      public PermissionCell getRequestCell(final int i) {
        return requestCells[i];
      }

      public class PermissionCell {
        private Permission myPermission;
        private final List<PermissionCell> parentCells;
        private final Optional<PermissionCell> grandParentCellOpt;
        private final Optional<PermissionCell> fallbackCellOpt;
        private final Optional<RequestKind> kindOpt;
        final boolean canBeUndecidable;

        public PermissionCell(final Optional<RequestKind> kind, final Permission startingPermission,
            final List<PermissionCell> parentCells,
            final Optional<PermissionCell> fallbackCellOpt, final Optional<PermissionCell> grandParentCellOpt,
            final boolean canBeUndecidable) {
          myPermission = startingPermission;
          this.parentCells = parentCells;
          this.fallbackCellOpt = fallbackCellOpt;
          this.grandParentCellOpt = grandParentCellOpt;
          this.kindOpt = kind;
          this.canBeUndecidable = canBeUndecidable;
        }

        public PermissionResult getEffectivePermission() {
          if (myPermission.isDecided()) {
            return new PermissionResult(myPermission, false);
          } else {
            final Permission firstCutPermission = computeFirstCutPermission();
            if (firstCutPermission.isDecided()) {
              return new PermissionResult(firstCutPermission, true);
            } else {
              assert (parentCells.size() > 0);
              return parentCells.get(0).getEffectivePermission().makeDefault();
            }
          }
        }

        private Permission computeFirstCutPermission() {
          if (parentExistsFor(Permission.Deny)) {
            return Permission.Deny;
          } else if (parentExistsFor(Permission.Allow)) {
            return Permission.Allow;
          } else {
            final Optional<PermissionResult> gpPermissionOpt = grandParentCellOpt.map(gp -> gp.getEffectivePermission());
            final boolean gpDefault = gpPermissionOpt.map(gp -> gp.isDefault).orElse(true);
            if (gpDefault) {
              return fallbackCellOpt.map(c -> c.getEffectivePermission().permission).orElse(Permission.Undecided);
            } else {
              return gpPermissionOpt.get().permission;
            }
          }
        }

        private boolean parentExistsFor(final Permission permission) {
          return parentCells.stream().anyMatch(cell -> {
            final PermissionResult effectivePermission = cell.getEffectivePermission();
            return (effectivePermission.permission == permission) && !effectivePermission.isDefault;
          });
        }

        public void setPermission(final Permission permission) {
          myPermission = permission;
          store.storePermissions(hostPattern, requestHost, kindOpt, permission);
        }
        
        // A method that returns the permission in here.
        public Permission getMyPermission() {
        	return myPermission;
        }
        
      }

    }

  }

  static Permission[] getEmptyPermissions() {
    final Map<RequestKind, Permission> emptyPermissionMap = new HashMap<>();
    final Permission[] emptyPermissions = flatten(emptyPermissionMap);
    return emptyPermissions;
  }

  public static Permission[] flatten(final Map<RequestKind, Permission> permissionMap) {
    final int numKinds = RequestKind.numKinds();
    final Permission[] permissions = new Permission[numKinds];
    IntStream.range(0, numKinds).forEach(i -> {
      permissions[i] = permissionMap.getOrDefault(RequestKind.forOrdinal(i), Permission.Undecided);
    });
    return permissions;
  }

}
