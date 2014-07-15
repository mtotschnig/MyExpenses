package org.totschnig.myexpenses.util;

import org.totschnig.myexpenses.MyApplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings.Secure;

import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.PreferenceObfuscator;

public class Distrib {

  public static PreferenceObfuscator getLicenseStatusPrefs(Context ctx) {
    String PREFS_FILE = "license_status";
    String deviceId = Secure.getString(ctx.getContentResolver(), Secure.ANDROID_ID);
    SharedPreferences sp = ctx.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    byte[] SALT = new byte[] {
        -1, -124, -4, -59, -52, 1, -97, -32, 38, 59, 64, 13, 45, -104, -3, -92, -56, -49, 65, -25
    };
    return new PreferenceObfuscator(
        sp, new AESObfuscator(SALT, ctx.getPackageName(), deviceId));
  }

  /**
   * @param ctx
   * @return -1 if we have a permanent license confirmed, otherwise the number of retrys returned from the licensing service
   */
  public static int getContribStatusInfo(Context ctx) {
    PreferenceObfuscator p = getLicenseStatusPrefs(ctx);
    if (p.getString(MyApplication.PrefKey.LICENSE_STATUS.getKey(),"0").equals("1"))
      return -1;
    else
      return Integer.parseInt(p.getString(
          MyApplication.PrefKey.LICENSE_RETRY_COUNT.getKey(),"0"));
  }

}
