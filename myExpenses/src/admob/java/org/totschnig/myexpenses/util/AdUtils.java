package org.totschnig.myexpenses.util;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.View;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;

/**
 * Created by michael on 04.06.15.
 */
public class AdUtils {
  private static String TAG = "AdUtils";
  private static InterstitialAd interstitialAd;
  public static boolean AD_DISABLED = Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO;
  public static void showBanner(View adView) {
    if (adView instanceof AdView) {
      AdRequest adRequest = new AdRequest.Builder().build();
      ((AdView) adView).loadAd(adRequest);
    } else {
      Log.e(TAG,"View must be of type AdView");
    }
  }
  public static void maybeRequestNewInterstitial(long now ,Context ctx) {
    if (now - MyApplication.PrefKey.INTERSTITIAL_LAST_SHOWN.getLong(0) > MyExpenses.DAY_IN_MILLIS &&
        MyApplication.PrefKey.ENTRIES_CREATED_SINCE_LAST_INTERSTITIAL.getInt(0)>9) {
      //last ad shown more than 24h and at least five expense entries ago,
      if (interstitialAd == null) {
        interstitialAd = new InterstitialAd(ctx);
        interstitialAd.setAdUnitId(ctx.getString(R.string.admob_unitid_interstitial));
      }
      AdRequest adRequest = new AdRequest.Builder()
          //.addTestDevice("YOUR_DEVICE_HASH")
          .build();
      interstitialAd.loadAd(adRequest);
    }
  }

  public static void maybeShowInterstitial(long now, Context ctx) {
    if (interstitialAd != null && interstitialAd.isLoaded()) {
      interstitialAd.show();
      MyApplication.PrefKey.INTERSTITIAL_LAST_SHOWN.putLong(now);
      MyApplication.PrefKey.ENTRIES_CREATED_SINCE_LAST_INTERSTITIAL.putInt(0);
    } else {
      MyApplication.PrefKey.ENTRIES_CREATED_SINCE_LAST_INTERSTITIAL.putInt(
          MyApplication.PrefKey.ENTRIES_CREATED_SINCE_LAST_INTERSTITIAL.getInt(0)+1
      );
      maybeRequestNewInterstitial(now,ctx);
    }
  }
}
