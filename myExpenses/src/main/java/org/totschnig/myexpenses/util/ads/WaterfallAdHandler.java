package org.totschnig.myexpenses.util.ads;

import android.support.annotation.Nullable;
import android.view.ViewGroup;

public class WaterfallAdHandler extends AdHandler {
  private final AdHandler[] cascade;
  private int cascadingIndex;
  private int cascadingIndexInterstitial;

  WaterfallAdHandler(AdHandlerFactory factory, ViewGroup adContainer, AdHandler... cascade) {
    super(factory, adContainer);
    this.cascade = cascade;
    for (AdHandler child : cascade) {
      child.setParent(this);
    }
  }

  @Override
  public void init() {
    if (shouldHideAd()) {
      super.hide();
    } else {
      cascadingIndex = 0;
      cascadingIndexInterstitial = 0;
      initCurrent();
    }
  }

  @Override
  protected boolean maybeShowInterstitialDo() {
    //This method will be called directly on the children via onEditTransactionResult,
    return false;
  }

  @Override
  protected void requestNewInterstitialDo() {
    requestNewInterstitialCurrent();
  }

  private boolean initCurrent() {
    AdHandler current = getCurrent();
    if (current != null) {
      current.init();
      return true;
    }
    return false;
  }

  @Nullable
  private AdHandler getCurrent() {
    return cascadingIndex < cascade.length ? cascade[cascadingIndex] : null;
  }

  @Nullable
  private AdHandler getCurrentForInterstitial() {
    return cascadingIndexInterstitial < cascade.length ? cascade[cascadingIndexInterstitial] : null;
  }

  @Override
  protected void hide() {
    cascadingIndex++;
    if (!initCurrent()) {
      super.hide();
    }
  }

  @Override
  protected void onInterstitialFailed() {
    cascadingIndexInterstitial++;
    requestNewInterstitialCurrent();
  }

  private void requestNewInterstitialCurrent() {
    AdHandler current = getCurrentForInterstitial();
    if (current != null) {
      current.requestNewInterstitialDo();
    }
  }

  public void onEditTransactionResult() {
    AdHandler current = getCurrentForInterstitial();
    if (current != null) {
      current.onEditTransactionResult();
    }
  }

  public void onResume() {
    AdHandler current = getCurrent();
    if (current != null) {
      current.onResume();
    }
  }

  public void onDestroy() {
    AdHandler current = getCurrent();
    if (current != null) {
      current.onDestroy();
    }
  }

  public void onPause() {
    AdHandler current = getCurrent();
    if (current != null) {
      current.onPause();
    }
  }
}
