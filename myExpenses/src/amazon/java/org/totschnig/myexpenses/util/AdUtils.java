package org.totschnig.myexpenses.util;

import android.util.Log;
import android.view.View;

import com.amazon.device.ads.AdLayout;
import com.amazon.device.ads.AdRegistration;

/**
 * Created by michael on 04.06.15.
 */
public class AdUtils {
  private static String TAG = "AdUtils";
  private static final String APP_KEY = "sample-app-v1_pub-2";
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
