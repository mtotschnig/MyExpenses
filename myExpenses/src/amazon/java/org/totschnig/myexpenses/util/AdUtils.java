package org.totschnig.myexpenses.util;

import android.util.Log;
import android.view.View;

import com.amazon.device.ads.AdLayout;
import com.amazon.device.ads.AdRegistration;

import org.totschnig.myexpenses.BuildConfig;

/**
 * Created by michael on 04.06.15.
 */
public class AdUtils {
  private static String TAG = "AdUtils";
  public static boolean AD_DISABLED = false; //should work also on Froyo
  private static final String APP_KEY = BuildConfig.DEBUG ?
      "sample-app-v1_pub-2" : "325c1c24185c46ccae8ec2cd4b2c290c";pu
  public static void showBanner(View adView) {
    if (adView instanceof AdLayout) {
      try {
        AdRegistration.setAppKey(APP_KEY);
      } catch (final IllegalArgumentException e) {
        Log.e(TAG, "IllegalArgumentException thrown: " + e.toString());
        return;
      }
      ((AdLayout) adView).loadAd();
    } else {
      Log.e(TAG,"View must be of type AdLayout");
    }
  }
}
