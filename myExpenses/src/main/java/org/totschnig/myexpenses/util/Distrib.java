package org.totschnig.myexpenses.util;

import java.util.ArrayList;

import org.onepf.oms.Appstore;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.AmazonAppstore;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.LaunchActivity;
import org.totschnig.myexpenses.contrib.Config;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings.Secure;
import android.util.Log;

import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.PreferenceObfuscator;

public class Distrib {

  public static boolean HAS_EXTENDED = !BuildConfig.FLAVOR_distribution.equals("blackberry");
  public static boolean IS_CHROMIUM = Build.BRAND.equals("chromium");

  public static final long REFUND_WINDOW = 172800000L;
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

  public static String STATUS_EXTENDED_TEMPORARY = "6";

  public static String STATUS_EXTENDED_PERMANENT = "7";

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
   * @param extended
   */
  public static void registerPurchase(Context ctx, boolean extended) {
    PreferenceObfuscator p = getLicenseStatusPrefs(ctx);
    String status = extended ? STATUS_EXTENDED_TEMPORARY : STATUS_ENABLED_TEMPORARY;
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
      if (timeSincePurchase> REFUND_WINDOW) {
        status = extended ? STATUS_EXTENDED_PERMANENT : STATUS_ENABLED_PERMANENT;
      }
    }
    p.putString(MyApplication.PrefKey.LICENSE_STATUS.getKey(), status);
    p.commit();
    MyApplication.getInstance().setContribStatus(status);
  }

  public static OpenIabHelper getIabHelper(Context ctx) {
    if (BuildConfig.FLAVOR_distribution.equals("blackberry")) {
      return null;
    }
    OpenIabHelper.Options.Builder builder =
        new OpenIabHelper.Options.Builder()
           .setVerifyMode(OpenIabHelper.Options.VERIFY_EVERYTHING)
           .addStoreKeys(Config.STORE_KEYS_MAP);

    if (IS_CHROMIUM) {
      builder.setStoreSearchStrategy(OpenIabHelper.Options.SEARCH_STRATEGY_BEST_FIT);
    }

    if (BuildConfig.FLAVOR_distribution.equals("amazon")) {
           ArrayList<Appstore> stores = new ArrayList<Appstore>();
           stores.add(new AmazonAppstore(ctx) {
             public boolean isBillingAvailable(String packageName) {
               return true;
             }
           });
           builder.addAvailableStores(stores);
    }

    return new OpenIabHelper(ctx,builder.build());
  }

  public static boolean isBatchAvailable() {
    return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD;
    }

  /**
   * After 2 days, if purchase cannot be verified, we set back
   * @param ctx
   */
  public static void maybeCancel(Context ctx) {
    PreferenceObfuscator p = getLicenseStatusPrefs(ctx);
    long timestamp = Long.parseLong(p.getString(
        MyApplication.PrefKey.LICENSE_INITIAL_TIMESTAMP.getKey(), "0"));
    long now = System.currentTimeMillis();
    long timeSincePurchase = now - timestamp;
    if (timeSincePurchase> REFUND_WINDOW) {
      String status = STATUS_DISABLED;
      p.putString(MyApplication.PrefKey.LICENSE_STATUS.getKey(), status);
      p.commit();
      MyApplication.getInstance().setContribStatus(status);
    }
  }
}
