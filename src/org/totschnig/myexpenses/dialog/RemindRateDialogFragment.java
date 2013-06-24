package org.totschnig.myexpenses.dialog;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class RemindRateDialogFragment  extends DialogFragment implements OnClickListener {

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    //tv.setMovementMethod(LinkMovementMethod.getInstance());
    return new AlertDialog.Builder(getActivity())
      .setTitle(R.string.app_name)
      .setMessage(R.string.dialog_remind_rate)
      .setCancelable(false)
      .setPositiveButton(R.string.dialog_remind_rate_yes, this)
      .setNeutralButton(R.string.dialog_remind_later,this)
      .setNegativeButton(R.string.dialog_remind_no,this)
      .create();
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (which == AlertDialog.BUTTON_POSITIVE)
      ((MessageDialogListener) getActivity())
        .dispatchCommand(R.id.RATE_COMMAND,null);
    else if (which == AlertDialog.BUTTON_NEUTRAL)
      ((MessageDialogListener) getActivity())
        .dispatchCommand(R.id.REMIND_LATER_COMMAND,"Rate");
    else
      ((MessageDialogListener) getActivity())
        .dispatchCommand(R.id.REMIND_NO_COMMAND,"Rate");
  }
}