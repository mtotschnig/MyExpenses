package org.totschnig.myexpenses.util.ads;

import android.view.ViewGroup;

public class WaterfallAdHandler extends AdHandler {
  private final AdHandler[] cascade;
  int cascadingIndex;

  public WaterfallAdHandler(ViewGroup adContainer, AdHandler... cascade) {
    super(adContainer);
    this.cascade = cascade;
  }

  @Override
  public void init() {
    cascadingIndex = 0;
    next();
  }

  private boolean next() {
    if (cascadingIndex <= cascade.length) {
      cascade[cascadingIndex].init();
      return true;
    }
    return false;
  }

  @Override
  protected void hide() {
    cascadingIndex++;
    if (!next()) {
      super.hide();
    }
  }
}
