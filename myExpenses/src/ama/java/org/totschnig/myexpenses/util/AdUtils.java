package org.totschnig.myexpenses.util;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;

import com.amazon.device.ads.AdLayout;
import com.amazon.device.ads.AdRegistration;
import com.amazon.device.ads.InterstitialAd;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;

/**
 * Created by michael on 04.06.15.
 */
public class AdUtils {
  private static String TAG = "AdUtils";
  private static InterstitialAd interstitialAd;
  private static final String APP_KEY = BuildConfig.DEBUG ?
      "sample-app-v1_pub-2" : "325c1c24185c46ccae8ec2cd4b2c290c";

  public static void showBanner(View adView) {
    if (adView instanceof AdLayout) {
      // For debugging purposes enable logging, but disable for production builds.
      AdRegistration.enableLogging(BuildConfig.DEBUG);
      // For debugging purposes flag all ad requests as tests, but set to false for production builds.
      AdRegistration.enableTesting(BuildConfig.DEBUG);
      try {
        AdRegistration.setAppKey(APP_KEY);
      } catch (final IllegalArgumentException e) {
        Log.e(TAG, "IllegalArgumentException thrown: " + e.toString());
        return;
      }
      if (!((AdLayout) adView).isLoading()) {
        ((AdLayout) adView).loadAd();
      }
    } else {
      Log.e(TAG, "View must be of type AdLayout");
    }
  }

  public static void requestNewInterstitial(Activity ctx) {
    if (interstitialAd == null) {
      interstitialAd = new InterstitialAd(ctx);
    }
    if (!interstitialAd.isLoading()) {
      interstitialAd.loadAd();
    }
  }

  public static boolean maybeShowInterstitial() {
    return interstitialAd != null && interstitialAd.showAd();
  }

  public static void resume(View adView) {
    //not handled
  }

  public static void pause(View adView) {
   //not handled
  }

  public static void destroy(View adView) {
    if (adView instanceof AdLayout) {
      ((AdLayout) adView).destroy();
    } else {
      Log.e(TAG, "View must be of type AdLayout");
    }
  }
}
