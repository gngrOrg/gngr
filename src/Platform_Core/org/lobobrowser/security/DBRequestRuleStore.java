package org.lobobrowser.security;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.javatuples.Pair;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.lobobrowser.security.PermissionSystem.Permission;
import org.lobobrowser.store.StorageManager;
import org.lobobrowser.ua.UserAgentContext.RequestKind;

import info.gngr.db.tables.Globals;
import info.gngr.db.tables.Permissions;
import info.gngr.db.tables.records.PermissionsRecord;

public class DBRequestRuleStore implements RequestRuleStore {
  private static final int BITS_PER_KIND = 2;
  private static final int HOST_SLOT = 0;
  private static final int MIN_KIND_SLOT = 1;
  static final int MAX_KIND_SLOT = (Integer.SIZE / BITS_PER_KIND) - 1;

  static {
    final RequestKind[] kinds = RequestKind.values();
    final int[] slots = new int[kinds.length];
    for (int i = 0; i < kinds.length; i++) {
      slots[i] = kinds[i].persistenceId;
    }
    validateSlots(slots);
  }

  private final DSLContext userDB;
  private static final Permission[] defaultPermissions = new Permission[RequestKind.numKinds()];
  static {
    for (int i = 0; i < defaultPermissions.length; i++) {
      defaultPermissions[i] = Permission.Undecided;
    }
  }
  private static final Pair<Permission, Permission[]> defaultPermissionPair = Pair.with(Permission.Undecided, defaultPermissions);

  private static class InstanceHolder {
    private static final DBRequestRuleStore INSTANCE = new DBRequestRuleStore();
  }

  public static DBRequestRuleStore getInstance() {
    return InstanceHolder.INSTANCE;
  }

  public DBRequestRuleStore() {
    this(StorageManager.getInstance().getDB());
  }

  DBRequestRuleStore(final DSLContext userDB) {
    this.userDB = userDB;
    if (!userDB.fetchOne(Globals.GLOBALS).getPermissionsinitialized()) {
      HelperPrivate.initStore(this);
      userDB.fetchOne(Globals.GLOBALS).setPermissionsinitialized(true);
    }
  }

  static int validateKindSlot(final int slot) {
    if ((slot < MIN_KIND_SLOT) || (slot > MAX_KIND_SLOT)) {
      throw new IllegalStateException(
          "RequestKind persistenceId out of 32-bit bitmask bounds [" + MIN_KIND_SLOT + ".." + MAX_KIND_SLOT + "]: " + slot);
    }
    return slot;
  }

  static void validateSlots(final int... slots) {
    final Set<Integer> seen = new HashSet<>();
    for (final int slot : slots) {
      validateKindSlot(slot);
      if (!seen.add(slot)) {
        throw new IllegalStateException("Duplicate RequestKind persistenceId slot: " + slot);
      }
    }
  }

  private static int bitPosition(final Optional<RequestKind> kindOpt) {
    return kindOpt.map(k -> validateKindSlot(k.persistenceId)).orElse(HOST_SLOT) * BITS_PER_KIND;
  }

  private static Condition matchHostsCondition(final String frameHost, final String requestHost) {
    return Permissions.PERMISSIONS.FRAMEHOST.equal(frameHost).and(Permissions.PERMISSIONS.REQUESTHOST.equal(requestHost));
  }

  public Pair<Permission, Permission[]> getPermissions(final String frameHostPattern, final String requestHost) {
    final Result<PermissionsRecord> permissionRecords = AccessController.doPrivileged((PrivilegedAction<Result<PermissionsRecord>>) () -> {
      return userDB.fetch(Permissions.PERMISSIONS, matchHostsCondition(frameHostPattern, requestHost));
    });

    if (permissionRecords.isEmpty()) {
      return defaultPermissionPair;
    } else {
      final PermissionsRecord existingRecord = permissionRecords.get(0);
      final Integer existingPermissions = existingRecord.getPermissions();
      final Pair<Permission, Permission[]> permissions = decodeBitMask(existingPermissions);
      return permissions;
    }
  }

  static Pair<Permission, Permission[]> decodeBitMask(final Integer existingPermissions) {
    final int mask = existingPermissions != null ? existingPermissions : 0;
    final Permission[] resultPermissions = new Permission[RequestKind.numKinds()];
    for (final RequestKind kind : RequestKind.values()) {
      resultPermissions[kind.ordinal()] = decodeBits(mask, validateKindSlot(kind.persistenceId));
    }
    final Pair<Permission, Permission[]> resultPair = Pair.with(decodeBits(mask, HOST_SLOT), resultPermissions);
    return resultPair;
  }

  static Permission decodeBits(final int existingPermissions, final int slot) {
    final int permissionBits = (existingPermissions >>> (slot * BITS_PER_KIND)) & 0x3;
    if (permissionBits < 2) {
      return Permission.Undecided;
    } else {
      return permissionBits == 0x3 ? Permission.Allow : Permission.Deny;
    }
  }

  public void storePermissions(final String frameHost, final String requestHost, final Optional<RequestKind> kindOpt,
      final Permission permission) {
    final Result<PermissionsRecord> permissionRecords = AccessController.doPrivileged((PrivilegedAction<Result<PermissionsRecord>>) () -> {
      return userDB.fetch(Permissions.PERMISSIONS, matchHostsCondition(frameHost, requestHost));
    });

    final int permissionMask = makeBitSetMask(kindOpt, permission);

    if (permissionRecords.isEmpty()) {
      final PermissionsRecord newPermissionRecord = new PermissionsRecord(frameHost, requestHost, permissionMask);
      newPermissionRecord.attach(userDB.configuration());
      newPermissionRecord.store();
    } else {
      final PermissionsRecord existingRecord = permissionRecords.get(0);
      final int existingPermissions = existingRecord.getPermissions() != null ? existingRecord.getPermissions() : 0;
      final int newPermissions = (existingPermissions & makeBitBlockMask(kindOpt)) | permissionMask;
      existingRecord.setPermissions(newPermissions);
      existingRecord.store();
    }
  }

  static int makeBitSetMask(final Optional<RequestKind> kindOpt, final Permission permission) {
    if (permission.isDecided()) {
      final int bitPos = bitPosition(kindOpt);
      final int bitset = permission == Permission.Allow ? 0x3 : 0x2;
      return bitset << bitPos;
    } else {
      return 0;
    }
  }

  static int makeBitBlockMask(final Optional<RequestKind> kindOpt) {
    final int bitPos = bitPosition(kindOpt);
    return ~(0x3 << bitPos);
  }
}