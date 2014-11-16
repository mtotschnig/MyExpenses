package org.totschnig.myexpenses.util;

import org.onepf.oms.appstore.googleUtils.Purchase;
import org.totschnig.myexpenses.MyApplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings.Secure;
import android.util.Log;

import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.PreferenceObfuscator;

public class Distrib {
  
  public static String STATUS_DISABLED = "0";
  
  /**
   * this status was used before and including the APP_GRATIS campaign
   */
  public static String STATUS_ENABLED_LEGACY_FIRST = "1";
  /**
   * this status was used after the APP_GRATIS campaign in order to distinguish
   * between free riders and buyers
   */
  public static String STATUS_ENABLED_LEGACY_SECOND = "2";

  /**
   * user has recently purchased, and is inside a two days window
   */
  public static String STATUS_ENABLED_TEMPORARY = "3";

  /**
   * user has recently purchased, and is inside a two days window
   */
  public static String STATUS_ENABLED_VERIFICATION_NEEDED = "4";
  
  /**
   * recheck passed
   */
  public static String STATUS_ENABLED_PERMANENT = "5";

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
   * @return -1,or-2 if we have a permanent license confirmed, otherwise the number of retrys returned from the licensing service
   */
  public static String getContribStatusInfo(Context ctx) {
    PreferenceObfuscator p = getLicenseStatusPrefs(ctx);
    return p.getString(MyApplication.PrefKey.LICENSE_STATUS.getKey(),STATUS_DISABLED);
  }

  /**
   * this is used from in-app billing
   * @param ctx
   * @param enabled
   */
  public static void registerPurchase(Context ctx) {
    PreferenceObfuscator p = getLicenseStatusPrefs(ctx);
    String status = STATUS_ENABLED_TEMPORARY;
    long timestamp = Long.parseLong(p.getString(
        MyApplication.PrefKey.LICENSE_INITIAL_TIMESTAMP.getKey(),"0"));
    long now = System.currentTimeMillis();
    if (timestamp == 0L) {
      p.putString(MyApplication.PrefKey.LICENSE_INITIAL_TIMESTAMP.getKey(),
          String.valueOf(now));
    } else {
      long timeSincePurchase = now - timestamp;
      Log.d(MyApplication.TAG,"time since initial check : " + timeSincePurchase);
        //give user 2 days to request refund
      if (timeSincePurchase> 172800000L) {
        status = STATUS_ENABLED_PERMANENT;
      }
    }
    p.putString(MyApplication.PrefKey.LICENSE_STATUS.getKey(), status);
    p.commit();
    MyApplication.getInstance().setContribStatus(status);
  }
  public static boolean isBatchAvailable() {
    return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD;
    }
}
