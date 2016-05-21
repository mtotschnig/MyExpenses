package org.totschnig.myexpenses.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;

public class PaypalPaymentCompletedCallbackDialog extends CommitSafeDialogFragment
    implements DialogInterface.OnClickListener {
  private static final String KEY_TRANSACTION_ID = "transaction_id";

  public static PaypalPaymentCompletedCallbackDialog newInstance(String transactionId) {
    PaypalPaymentCompletedCallbackDialog fragment = new PaypalPaymentCompletedCallbackDialog();
    Bundle args = new Bundle();
    args.putString(KEY_TRANSACTION_ID, transactionId);
    fragment.setArguments(args);
    fragment.setCancelable(false);
    return fragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    return builder.setTitle(R.string.thank_you)
        .setMessage(R.string.paypal_callback_info)
        .setPositiveButton(R.string.pref_request_licence_title, this)
        .setNegativeButton(R.string.dialog_remind_later, this)
        .create();
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (which == AlertDialog.BUTTON_POSITIVE) {
      ((ProtectedFragmentActivity) getActivity()).dispatchCommand(R.id.REQUEST_LICENCE_COMMAND,
          getArguments().getString(KEY_TRANSACTION_ID));
    } else if (which == AlertDialog.BUTTON_NEGATIVE) {
      Toast.makeText(getActivity(),R.string.paypal_callback_later_clicked_info,Toast.LENGTH_LONG).show();
    }
    getActivity().finish();
  }
}
