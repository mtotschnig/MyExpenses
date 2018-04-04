package org.totschnig.myexpenses.ui;

import android.view.View;

public class SnackbarAction {
  public final int resId;
  public final View.OnClickListener listener;

  public SnackbarAction(int resId, View.OnClickListener listener) {
    this.resId = resId;
    this.listener = listener;
  }
}
