package org.totschnig.myexpenses.dialog;

import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Button;

public class ButtonOnShowDisabler implements DialogInterface.OnShowListener {
  @Override
  public void onShow(DialogInterface dialog) {
    Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
    if (button != null) {
      button.setEnabled(false);
    }
  }
}