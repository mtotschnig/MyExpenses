package org.totschnig.myexpenses.util.ads;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.util.tracking.Tracker;

import javax.inject.Inject;

public abstract class AdHandler {
  protected static final int DAY_IN_MILLIS = BuildConfig.DEBUG ? 1 : 86400000;
  private static final int INITIAL_GRACE_DAYS = BuildConfig.DEBUG ? 0 : 5;
  private static final String AD_TYPE_BANNER = "banner";
  private static final String AD_TYPE_INTERSTITIAL = "interstitial";
  protected final ViewGroup adContainer;
  protected Context context;
  @Inject
  protected Tracker tracker;

  protected AdHandler(ViewGroup adContainer) {
    this.adContainer = adContainer;
    this.context = adContainer.getContext();
    MyApplication.getInstance().getAppComponent().inject(this);
  }

  public abstract void init();

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

  public void onEditTransactionResult() {

  }

  public void onResume() {

  }

  public void onDestroy() {

  }

  public void onPause() {

  }

  protected final void hide() {
    adContainer.setVisibility(View.GONE);
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
}
