package org.totschnig.myexpenses.util.ads;

import android.view.ViewGroup;

public class NoOpAdHandler extends AdHandler {
  public NoOpAdHandler(AdHandlerFactory defaultAdHandlerFactory, ViewGroup adContainer) {
    super(defaultAdHandlerFactory, adContainer);
  }

  public void init() {
    hide();
  }

  @Override
  public void maybeRequestNewInterstitial() {}

  @Override
  protected void maybeShowInterstitial() {}

  @Override
  protected boolean maybeShowInterstitialDo() {
    return false;
  }

  @Override
  protected void requestNewInterstitialDo() {}
}
