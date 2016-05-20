package org.totschnig.myexpenses.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

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
    return fragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    return builder.setTitle(R.string.thank_you)
        .setMessage("Your transaction has been completed, and a receipt for your purchase has been emailed to you." +
            "You may log into your account at www.paypal.com to view details of this transaction.")
        .setPositiveButton(R.string.pref_request_licence_title, this)
        .create();

  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (which == AlertDialog.BUTTON_POSITIVE) {
      ((ProtectedFragmentActivity) getActivity()).dispatchCommand(R.id.REQUEST_LICENCE_COMMAND,
          getArguments().getString(KEY_TRANSACTION_ID));
    }
  }
}
