package org.totschnig.myexpenses.util;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import org.totschnig.myexpenses.R;

public class AdHandler {

  private final ViewGroup adContainer;

  public AdHandler(Activity activity) {
    this.adContainer = (ViewGroup) activity.findViewById(R.id.adContainer);
  }

  public void init() {
    adContainer.setVisibility(View.GONE);
  }


  public void onEditTransactionResult() {
  }

  public void onResume() {
  }

  public void onDestroy() {
  }

  public void onPause() {
  }
}
