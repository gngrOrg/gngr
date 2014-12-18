package org.lobobrowser.security;

import java.util.Optional;

import org.javatuples.Pair;
import org.lobobrowser.security.PermissionSystem.Permission;
import org.lobobrowser.ua.UserAgentContext.RequestKind;

interface RequestRuleStore {
  public Pair<Permission, Permission[]> getPermissions(final String frameHostPattern, final String requestHost);

  public void storePermissions(final String frameHost, final String requestHost, Optional<RequestKind> kindOpt, Permission permission);

  public static RequestRuleStore getStore() {
    // return InMemoryStore.getInstance();
    return DBRequestRuleStore.getInstance();
  }

  static class HelperPrivate {
    static void initStore(final RequestRuleStore store) {
      final Pair<Permission, Permission[]> permissions = store.getPermissions("*", "");
      assert (!permissions.getValue0().isDecided());
      store.storePermissions("*", "", Optional.empty(), Permission.Deny);
      store.storePermissions("*", "", Optional.of(RequestKind.Image), Permission.Allow);
      store.storePermissions("*", "", Optional.of(RequestKind.CSS), Permission.Allow);
    }
  }
}
