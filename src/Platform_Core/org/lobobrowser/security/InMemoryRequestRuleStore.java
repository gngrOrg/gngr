package org.lobobrowser.security;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.javatuples.Pair;
import org.lobobrowser.security.PermissionSystem.Permission;
import org.lobobrowser.ua.UserAgentContext.RequestKind;

public class InMemoryRequestRuleStore implements RequestRuleStore {
  private final Map<String, Map<String, Permission[]>> store = new HashMap<>();
  private static final Permission[] defaultPermissions = new Permission[RequestKind.numKinds() + 1];
  static {
    for (int i = 0; i < defaultPermissions.length; i++) {
      defaultPermissions[i] = Permission.Undecided;
    }
  }
  private static final Pair<Permission, Permission[]> defaultPermissionPair = Pair.with(Permission.Undecided, defaultPermissions);

  static private InMemoryRequestRuleStore instance = new InMemoryRequestRuleStore();

  public static InMemoryRequestRuleStore getInstance() {
    instance.dump();
    return instance;
  }

  public InMemoryRequestRuleStore() {
    HelperPrivate.initStore(this);
  }

  public synchronized Pair<Permission, Permission[]> getPermissions(final String frameHostPattern, final String requestHost) {
    final Map<String, Permission[]> reqHostMap = store.get(frameHostPattern);
    if (reqHostMap != null) {
      final Permission[] permissions = reqHostMap.get(requestHost);
      if (permissions != null) {
        return Pair.with(permissions[0], Arrays.copyOfRange(permissions, 1, permissions.length));
      } else {
        return defaultPermissionPair;
      }
    } else {
      return defaultPermissionPair;
    }
  }

  public synchronized void storePermissions(final String frameHostPattern, final String requestHost, final Optional<RequestKind> kindOpt,
      final Permission permission) {
    final int index = kindOpt.map(k -> k.ordinal() + 1).orElse(0);
    final Map<String, Permission[]> reqHostMap = store.get(frameHostPattern);
    if (reqHostMap != null) {
      final Permission[] permissions = reqHostMap.get(requestHost);
      if (permissions != null) {
        permissions[index] = permission;
      } else {
        addPermission(requestHost, index, permission, reqHostMap);
      }
    } else {
      final Map<String, Permission[]> newReqHostMap = new HashMap<>();
      addPermission(requestHost, index, permission, newReqHostMap);
      store.put(frameHostPattern, newReqHostMap);
    }
  }

  private static void addPermission(final String requestHost, final int index, final Permission permission,
      final Map<String, Permission[]> reqHostMap) {
    final Permission[] newPermissions = Arrays.copyOf(defaultPermissions, defaultPermissions.length);
    newPermissions[index] = permission;
    reqHostMap.put(requestHost, newPermissions);
  }

  private void dump() {
    System.out.println("Store: ");
    store.forEach((key, value) -> {
      System.out.println("{" + key + ": ");
      value.forEach((key2, value2) -> {
        System.out.println("  " + key2 + ": " + Arrays.toString(value2));
      });
      System.out.println("}");
    });
  }
}