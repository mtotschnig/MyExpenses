package org.totschnig.myexpenses.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.preference.PrefKey;

import java.io.File;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class PermissionHelper {
  public static final int PERMISSIONS_REQUEST_WRITE_CALENDAR = 1;
  public static final int PERMISSIONS_REQUEST_STORAGE = 2;

  private PermissionHelper() {}

  public enum PermissionGroup {
    STORAGE(externalReadPermissionCompat(), PrefKey.STORAGE_PERMISSION_REQUESTED, PERMISSIONS_REQUEST_STORAGE),
    CALENDAR(new String[]{Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR}, PrefKey.CALENDAR_PERMISSION_REQUESTED, PERMISSIONS_REQUEST_WRITE_CALENDAR);

    public final String[] androidPermissions;
    public final PrefKey prefKey;
    public final int requestCode;

    PermissionGroup(String[] androidPermission, PrefKey prefKey, int requestCode) {
      this.androidPermissions = androidPermission;
      this.prefKey = prefKey;
      this.requestCode = requestCode;
    }

    public static PermissionGroup fromRequestCode(int requestCode) {
      if (requestCode == STORAGE.requestCode) return STORAGE;
      if (requestCode == CALENDAR.requestCode) return CALENDAR;
      throw new IllegalArgumentException("Undefined requestCode " + requestCode);
    }

    /**
     *
     * @param context
     * @return true if all of our {@link #androidPermissions} are granted
     */
    public boolean hasPermission(Context context) {
      for (String permission: androidPermissions) {
        if (!PermissionHelper.hasPermission(context, permission)) {
          return false;
        }
      }
      return true;
    }

    /**
     *
     * @param context
     * @return true if {@link ActivityCompat#shouldShowRequestPermissionRationale(Activity, String)}
     * returns true for any of our {@link #androidPermissions}
     */
    public boolean shouldShowRequestPermissionRationale(Activity context) {
      for (String permission: androidPermissions) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(context, permission)) {
          return true;
        }
      }
      return false;
    }
  }

  public static boolean hasPermission(Context context, String permission) {
    return getSelfPermissionSafe(context, permission) == PERMISSION_GRANTED;
  }

  public static boolean hasCalendarPermission(Context context) {
    return PermissionGroup.CALENDAR.hasPermission(context);
  }

  public static boolean hasExternalReadPermission(Context context) {
    return PermissionGroup.STORAGE.hasPermission(context);
  }

  /**
   * Wrapper around {@link ContextCompat#checkSelfPermission(Context, String)}
   * Workaround for RuntimeException
   * See https://github.com/permissions-dispatcher/PermissionsDispatcher/pull/108/files
   * @param context context
   * @param permission permission
   * @return returns true if context has access to the given permission, false otherwise.
   */
  public static int getSelfPermissionSafe(Context context, String permission) {
    try {
      return ContextCompat.checkSelfPermission(context, permission);
    } catch (RuntimeException t) {
      return PERMISSION_DENIED;
    }
  }

  public static String[] externalReadPermissionCompat() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      return new String[] {Manifest.permission.READ_EXTERNAL_STORAGE};
    } else {
      return new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }
  }

  public static boolean canReadUri(Uri uri, Context context) {
    if ("file".equals(uri.getScheme())) {
      File file = new File(uri.getPath());
      return file.exists() && file.canRead();
    }
    return AppDirHelper.getFileProviderAuthority().equals(uri.getAuthority()) ||
        hasExternalReadPermission(context) ||
        context.checkUriPermission(uri, Binder.getCallingPid(), Binder.getCallingUid(),
            Intent.FLAG_GRANT_READ_URI_PERMISSION) == PERMISSION_GRANTED;
  }

  public static PrefKey permissionRequestedKey(int requestCode) {
    return PermissionGroup.fromRequestCode(requestCode).prefKey;
  }

  public static CharSequence permissionRequestRationale(Context context, int requestCode) {
    switch (requestCode) {
      case PERMISSIONS_REQUEST_WRITE_CALENDAR:
        return Utils.getTextWithAppName(context, R.string.calendar_permission_required);
      case PERMISSIONS_REQUEST_STORAGE:
        return context.getString(R.string.storage_permission_required);
    }
    throw new IllegalArgumentException("Undefined requestCode " + requestCode);
  }

  public static boolean allGranted(int[] grantResults) {
    if (grantResults.length == 0) {
      return false;
    }
    for (int result: grantResults) {
      if (result != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }
}
