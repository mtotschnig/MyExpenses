package org.totschnig.myexpenses.util.ads;

public class NoOpAdHandler extends AdHandler {
  NoOpAdHandler() {
    super(null, null);
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
