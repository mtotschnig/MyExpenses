package org.totschnig.myexpenses.util.ads;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.tracking.Tracker;

import javax.inject.Inject;

public abstract class AdHandler {
  private static final int INTERSTITIAL_MIN_INTERVAL = BuildConfig.DEBUG ? 2 : 4;
  protected static final int DAY_IN_MILLIS = BuildConfig.DEBUG ? 1 : 86400000;
  private static final int INITIAL_GRACE_DAYS = BuildConfig.DEBUG ? 0 : 5;
  private static final String AD_TYPE_BANNER = "banner";
  private static final String AD_TYPE_INTERSTITIAL = "interstitial";
  protected final ViewGroup adContainer;
  protected Context context;
  @Inject
  protected Tracker tracker;
  private AdHandler parent;

  protected AdHandler(ViewGroup adContainer) {
    this.adContainer = adContainer;
    this.context = adContainer.getContext();
    MyApplication.getInstance().getAppComponent().inject(this);
  }

  public abstract void init();

  public void maybeRequestNewInterstitial() {
    long now = System.currentTimeMillis();
    if (now - PrefKey.INTERSTITIAL_LAST_SHOWN.getLong(0) > DAY_IN_MILLIS &&
        PrefKey.ENTRIES_CREATED_SINCE_LAST_INTERSTITIAL.getInt(0) > INTERSTITIAL_MIN_INTERVAL) {
      //last ad shown more than 24h and at least five expense entries ago,
      requestNewInterstitialDo();
    }
  }

  protected void maybeShowInterstitial() {
    if (maybeShowInterstitialDo()) {
      PrefKey.INTERSTITIAL_LAST_SHOWN.putLong(System.currentTimeMillis());
      PrefKey.ENTRIES_CREATED_SINCE_LAST_INTERSTITIAL.putInt(0);
    } else {
      PrefKey.ENTRIES_CREATED_SINCE_LAST_INTERSTITIAL.putInt(
          PrefKey.ENTRIES_CREATED_SINCE_LAST_INTERSTITIAL.getInt(0) + 1
      );
      maybeRequestNewInterstitial();
    }
  }

  protected abstract boolean maybeShowInterstitialDo();

  protected abstract void requestNewInterstitialDo();

  protected boolean isAdDisabled() {
    return isAdDisabled(context);
  }

  public static boolean isAdDisabled(Context context) {
    return BuildConfig.DEBUG ||
        (ContribFeature.AD_FREE.hasAccess() ||
            isInInitialGracePeriod(context));
  }

  private static boolean isInInitialGracePeriod(Context context) {
    try {
      return System.currentTimeMillis() -
          context.getPackageManager().getPackageInfo(context.getPackageName(), 0)
              .firstInstallTime < DAY_IN_MILLIS * INITIAL_GRACE_DAYS;
    } catch (PackageManager.NameNotFoundException e) {
      return false;
    }
  }

  protected void onInterstitialFailed() {
    if (parent != null) {
      parent.onInterstitialFailed();
    }
  }

  public void onEditTransactionResult() {
    if (!isAdDisabled()) {
      maybeShowInterstitial();
    }
  }

  public void onResume() {}

  public void onDestroy() {}

  public void onPause() {}

  protected void hide() {
    if (parent != null) {
      parent.hide();
    } else {
      adContainer.setVisibility(View.GONE);
    }
  }

  protected final void trackBannerRequest(String provider) {
    track(Tracker.EVENT_AD_REQUEST, AD_TYPE_BANNER, provider);
  }

  protected final void trackInterstitialRequest(String provider) {
    track(Tracker.EVENT_AD_REQUEST, AD_TYPE_INTERSTITIAL, provider);
  }

  protected final void trackBannerLoaded(String provider) {
    track(Tracker.EVENT_AD_LOADED, AD_TYPE_BANNER, provider);
  }

  protected final void trackInterstitialLoaded(String provider) {
    track(Tracker.EVENT_AD_LOADED, AD_TYPE_INTERSTITIAL, provider);
  }

  protected final void trackBannerFailed(String provider, String errorCode) {
    track(Tracker.EVENT_AD_FAILED, AD_TYPE_BANNER, provider, errorCode);
  }

  protected final void trackInterstitialFailed(String provider, String errorCode) {
    track(Tracker.EVENT_AD_FAILED, AD_TYPE_INTERSTITIAL, provider, errorCode);
  }

  protected final void trackInterstitialShown(String provider) {
    track(Tracker.EVENT_AD_SHOWN, AD_TYPE_INTERSTITIAL, provider);
  }

  private void track(String event, String type, String provider) {
    track(event, type, provider, null);
  }

  private void track(String event, String type, String provider, String errorCode) {
    tracker.logEvent(event, buildBundle(type, provider, errorCode));
  }

  private Bundle buildBundle(String type, String provider, String errorCode) {
    Bundle bundle = new Bundle(errorCode == null ? 2 : 3);
    bundle.putString(Tracker.EVENT_PARAM_AD_TYPE, type);
    bundle.putString(Tracker.EVENT_PARAM_AD_PROVIDER, provider);
    if (errorCode != null) {
      bundle.putString(Tracker.EVENT_PARAM_AD_ERROR_CODE, errorCode);
    }
    return bundle;
  }

  public void setParent(AdHandler parent) {
    this.parent = parent;
  }

  public AdHandler getParent() {
    return parent;
  }
}
