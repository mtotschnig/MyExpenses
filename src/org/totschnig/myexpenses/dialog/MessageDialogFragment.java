package org.totschnig.myexpenses.dialog;

import java.io.Serializable;

import org.totschnig.myexpenses.MyApplication;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class MessageDialogFragment  extends DialogFragment{

  public static final MessageDialogFragment newInstance(int title, int message, int command, Serializable tag) {
    return newInstance(title, MyApplication.getInstance().getString(message), command, tag, android.R.string.yes, android.R.string.no);
  }
  public static final MessageDialogFragment newInstance(int title, CharSequence message, int command, Serializable tag) {
    return newInstance(title, message, command, tag, android.R.string.yes, android.R.string.no);
  }
  public static final MessageDialogFragment newInstance(int title, CharSequence message, int command, Serializable tag,
      int yesButton, int noButton) {
    MessageDialogFragment dialogFragment = new MessageDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putInt("title", title);
    bundle.putCharSequence("message", message);
    bundle.putInt("command", command);
    bundle.putSerializable("tag", tag);
    bundle.putInt("yesButton",yesButton);
    bundle.putInt("noButton",noButton);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
      final Bundle bundle = getArguments();
      AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
      alertDialogBuilder.setTitle(bundle.getInt("title"));
      alertDialogBuilder.setMessage(bundle.getCharSequence("message"));
      //null should be your on click listener
      alertDialogBuilder.setNegativeButton(bundle.getInt("noButton"), new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
          onCancel(dialog);
        }
    });
      alertDialogBuilder.setPositiveButton(bundle.getInt("yesButton"), new DialogInterface.OnClickListener() {

          @Override
          public void onClick(DialogInterface dialog, int which) {
              //dialog.dismiss();
              if (which == AlertDialog.BUTTON_POSITIVE)
                ((MessageDialogListener) getActivity())
                .dispatchCommand(bundle.getInt("command"), bundle.getSerializable("tag"));
          }
      });
      return alertDialogBuilder.create();
  }
  public void onCancel (DialogInterface dialog) {
    ((MessageDialogListener) getActivity()).cancelDialog();
  }
  public interface MessageDialogListener {
    boolean dispatchCommand(int mCommand, Object mTag);

    void cancelDialog();
}
}