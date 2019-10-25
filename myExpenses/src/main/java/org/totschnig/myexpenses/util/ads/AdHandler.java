package org.totschnig.myexpenses.util.ads;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.util.tracking.Tracker;

import javax.inject.Inject;

import timber.log.Timber;

import static android.text.format.DateUtils.DAY_IN_MILLIS;
import static org.totschnig.myexpenses.preference.PrefKey.ENTRIES_CREATED_SINCE_LAST_INTERSTITIAL;
import static org.totschnig.myexpenses.preference.PrefKey.INTERSTITIAL_LAST_SHOWN;
import static org.totschnig.myexpenses.preference.PrefKey.PERSONALIZED_AD_CONSENT;

public abstract class AdHandler {
  private static final int INTERSTITIAL_MIN_INTERVAL = 4;
  private static final String AD_TYPE_BANNER = "banner";
  private static final String AD_TYPE_INTERSTITIAL = "interstitial";
  private final AdHandlerFactory factory;
  protected final ViewGroup adContainer;
  protected Context context;
  @Inject
  protected Tracker tracker;
  @Inject
  protected PrefHandler prefHandler;
  private AdHandler parent;
  private boolean initialized;

  protected AdHandler(AdHandlerFactory factory, ViewGroup adContainer) {
    this.factory = factory;
    this.adContainer = adContainer;
    this.context = adContainer.getContext().getApplicationContext();
    ((MyApplication) context).getAppComponent().inject(this);
  }

  protected final void init() {
    if (!initialized) {
      _init();
      initialized = true;
    }
  }
  public void startBanner() {
    try {
      init();
      if (shouldHideAd()) {
        hide();
      } else {
        _startBanner();
      }
    } catch (Exception e) {
      Timber.e(e);
    }
  }

  protected void _init() {}

  protected abstract void _startBanner();

  public void maybeRequestNewInterstitial() {
    long now = System.currentTimeMillis();
    if (now - prefHandler.getLong(INTERSTITIAL_LAST_SHOWN,0) > DAY_IN_MILLIS &&
        prefHandler.getInt(ENTRIES_CREATED_SINCE_LAST_INTERSTITIAL, 0) > INTERSTITIAL_MIN_INTERVAL) {
      //last ad shown more than 24h and at least five expense entries ago,
      requestNewInterstitialDo();
    }
  }

  protected void maybeShowInterstitial() {
    if (maybeShowInterstitialDo()) {
      prefHandler.putLong(INTERSTITIAL_LAST_SHOWN, System.currentTimeMillis());
      prefHandler.putInt(ENTRIES_CREATED_SINCE_LAST_INTERSTITIAL, 0);
    } else {
      prefHandler.putInt(ENTRIES_CREATED_SINCE_LAST_INTERSTITIAL,
          prefHandler.getInt(ENTRIES_CREATED_SINCE_LAST_INTERSTITIAL, 0) + 1);
      maybeRequestNewInterstitial();
    }
  }

  protected abstract boolean maybeShowInterstitialDo();

  protected abstract void requestNewInterstitialDo();

  boolean shouldHideAd() {
    return factory.isAdDisabled() || (factory.isRequestLocationInEeaOrUnknown() && !prefHandler.isSet(PERSONALIZED_AD_CONSENT));
  }

  protected void onInterstitialFailed() {
    if (parent != null) {
      parent.onInterstitialFailed();
    }
  }

  public void onEditTransactionResult() {
    try {
      if (!shouldHideAd()) {
        init();
        maybeShowInterstitial();
      }
    } catch (Exception e) {
      Timber.e(e);
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
