package org.totschnig.myexpenses.dialog;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.util.Utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class ContribInfoDialogFragment  extends DialogFragment implements OnClickListener {

  /**
   * @param reminderP yes if we are called from a reminder
   * @return
   */
  public static final ContribInfoDialogFragment newInstance(boolean reminderP) {
    ContribInfoDialogFragment dialogFragment = new ContribInfoDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putBoolean("reminderP", reminderP);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    //tv.setMovementMethod(LinkMovementMethod.getInstance());
    AlertDialog.Builder builder =  new AlertDialog.Builder(getActivity())
      .setTitle(R.string.menu_contrib);
    if (MyApplication.getInstance().isContribEnabled) {
      builder.setMessage(R.string.dialog_contrib_thanks)
        .setNeutralButton(android.R.string.ok, null);
    } else {
      builder.setMessage(Html.fromHtml(getString(R.string.dialog_contrib_text,Utils.getContribFeatureLabelsAsFormattedList(getActivity()))))
        .setPositiveButton(R.string.dialog_contrib_yes, this);
      if (getArguments().getBoolean("reminderP")) {
        builder.setCancelable(false)
          .setNeutralButton(R.string.dialog_remind_later,this)
          .setNegativeButton(R.string.dialog_remind_no,this);
      } else {
        builder.setNegativeButton(R.string.dialog_contrib_no,null);
      }
    }  
    return builder.create();
  }
  @Override
  public void onStart() {
    super.onStart();
    if (MyApplication.getInstance().isContribEnabled) {
      Dialog dlg = getDialog();
      TextView message = (TextView) dlg.findViewById(android.R.id.message);
      message.setLinkTextColor(getActivity().getResources().getColorStateList(R.drawable.linkcolor));
      message.setMovementMethod(LinkMovementMethod.getInstance());
    }
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (which == AlertDialog.BUTTON_POSITIVE)
      ((MessageDialogListener) getActivity())
        .dispatchCommand(R.id.CONTRIB_PLAY_COMMAND,null);
    else if (which == AlertDialog.BUTTON_NEUTRAL)
      ((MessageDialogListener) getActivity())
        .dispatchCommand(R.id.REMIND_LATER_COMMAND,"Contrib");
    else
      ((MessageDialogListener) getActivity())
        .dispatchCommand(R.id.REMIND_NO_COMMAND,"Contrib");
  }
}