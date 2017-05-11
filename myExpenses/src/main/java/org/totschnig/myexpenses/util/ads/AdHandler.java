package org.totschnig.myexpenses.util.ads;

import android.view.View;
import android.view.ViewGroup;

public abstract class AdHandler {
  protected final ViewGroup adContainer;
  protected AdHandler(ViewGroup adContainer) {
    this.adContainer = adContainer;
  }

  public void init() {

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
}
