package org.totschnig.myexpenses.util;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.support.v4.content.ContextCompat;

import java.io.File;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class PermissionHelper {
  private PermissionHelper() {}

  public static boolean hasPermission(Context context, String permission) {
    return ContextCompat.checkSelfPermission(context, permission) == PERMISSION_GRANTED;
  }

  public static boolean hasExternalReadPermission(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      return hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
    } else {
      return hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }
  }

  public static boolean canReadUri(Uri uri, Context context) {
    switch(uri.getScheme()) {
      case "file":
        File file = new File(uri.getPath());
        return file.exists() && file.canRead();
      default:
        return hasExternalReadPermission(context) ||
            context.checkUriPermission(uri, Binder.getCallingPid(), Binder.getCallingUid(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION) == PERMISSION_GRANTED;
    }
  }
}
