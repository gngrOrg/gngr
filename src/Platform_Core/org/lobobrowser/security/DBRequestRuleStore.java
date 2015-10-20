package org.lobobrowser.security;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Optional;

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
  private final DSLContext userDB;
  private static final Permission[] defaultPermissions = new Permission[RequestKind.numKinds()];
  static {
    for (int i = 0; i < defaultPermissions.length; i++) {
      defaultPermissions[i] = Permission.Undecided;
    }
  }
  private static final Pair<Permission, Permission[]> defaultPermissionPair = Pair.with(Permission.Undecided, defaultPermissions);

  static private DBRequestRuleStore instance = new DBRequestRuleStore();

  public static DBRequestRuleStore getInstance() {
    return instance;
  }

  public DBRequestRuleStore() {
    final StorageManager storageManager = StorageManager.getInstance();
    userDB = storageManager.getDB();
    if (!userDB.fetchOne(Globals.GLOBALS).getPermissionsinitialized()) {
      HelperPrivate.initStore(this);
      userDB.fetchOne(Globals.GLOBALS).setPermissionsinitialized(true);
    }
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

  private static Pair<Permission, Permission[]> decodeBitMask(final Integer existingPermissions) {
    final Permission[] resultPermissions = new Permission[RequestKind.numKinds()];
    for (int i = 0; i < resultPermissions.length; i++) {
      resultPermissions[i] = decodeBits(existingPermissions, i + 1);
    }
    final Pair<Permission, Permission[]> resultPair = Pair.with(decodeBits(existingPermissions, 0), resultPermissions);
    return resultPair;
  }

  private static final int BITS_PER_KIND = 2;

  private static Permission decodeBits(final Integer existingPermissions, final int i) {
    final int permissionBits = (existingPermissions >> (i * BITS_PER_KIND)) & 0x3;
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

    final Integer permissionMask = makeBitSetMask(kindOpt, permission);

    if (permissionRecords.isEmpty()) {
      final PermissionsRecord newPermissionRecord = new PermissionsRecord(frameHost, requestHost, permissionMask);
      newPermissionRecord.attach(userDB.configuration());
      newPermissionRecord.store();
    } else {
      final PermissionsRecord existingRecord = permissionRecords.get(0);
      final Integer existingPermissions = existingRecord.getPermissions();
      final int newPermissions = (existingPermissions & makeBitBlockMask(kindOpt)) | permissionMask;
      existingRecord.setPermissions(newPermissions);
      existingRecord.store();
    }
  }

  private static Integer makeBitSetMask(final Optional<RequestKind> kindOpt, final Permission permission) {
    if (permission.isDecided()) {
      final Integer bitPos = kindOpt.map(k -> k.ordinal() + 1).orElse(0) * BITS_PER_KIND;
      final int bitset = permission == Permission.Allow ? 0x3 : 0x2;
      return bitset << bitPos;
    } else {
      return 0;
    }
  }

  private static Integer makeBitBlockMask(final Optional<RequestKind> kindOpt) {
    final Integer bitPos = kindOpt.map(k -> k.ordinal() + 1).orElse(0) * BITS_PER_KIND;
    return ~(0x3 << bitPos);
  }
}