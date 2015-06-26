package org.totschnig.myexpenses.util;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import org.jetbrains.annotations.NotNull;
import org.totschnig.myexpenses.R;

/**
 * Created by michael on 04.06.15.
 */
public class AdUtils {
  private static String TAG = "AdUtils";
  private static InterstitialAd interstitialAd;

  public static void showBanner(View adView) {
    if (adView instanceof AdView) {
      ((AdView) adView).loadAd(buildRequest());
    } else {
      Log.e(TAG, "View must be of type AdView");
    }
  }

  public static void requestNewInterstitial(Context ctx) {
    if (interstitialAd == null) {
      interstitialAd = new InterstitialAd(ctx);
      interstitialAd.setAdUnitId(ctx.getString(R.string.admob_unitid_interstitial));
    }
    if (!interstitialAd.isLoaded()) {
      interstitialAd.loadAd(buildRequest());
    }
  }

  @NotNull
  private static AdRequest buildRequest() {
    return new AdRequest.Builder()
        .addTestDevice("B6E7D9A3244EDB91BDB897A53A1B02C4")
        .build();
  }

  public static boolean maybeShowInterstitial() {
    if (interstitialAd != null && interstitialAd.isLoaded()) {
      interstitialAd.show();
      return true;
    }
    return false;
  }

  public static void resume(View adView) {
    if (adView instanceof AdView) {
      ((AdView) adView).resume();
    } else {
      Log.e(TAG, "View must be of type AdView");
    }
  }

  public static void pause(View adView) {
    if (adView instanceof AdView) {
      ((AdView) adView).pause();
    } else {
      Log.e(TAG, "View must be of type AdView");
    }
  }

  public static void destroy(View adView) {
    if (adView instanceof AdView) {
      ((AdView) adView).destroy();
    } else {
      Log.e(TAG, "View must be of type AdView");
    }
  }
}
