package org.totschnig.myexpenses.dialog;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Could be generalized to handle any uri and a generic callback interface like MessageDialogFragment
 *
 */
public class SelectAccountDialogFragment extends DialogFragment implements OnClickListener {
  
  public static final SelectAccountDialogFragment newInstance(Long fromAccountId, Long transactionId) {
    SelectAccountDialogFragment dialogFragment = new SelectAccountDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putLong("fromAccountId", fromAccountId);
    bundle.putLong("transactionId", transactionId);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Context ctx = getActivity();
    return new AlertDialog.Builder(ctx)
      .setTitle(R.string.dialog_title_select_account)
      .setSingleChoiceItems(
          ctx.getContentResolver().query(
              TransactionProvider.ACCOUNTS_URI,
              new String[] {KEY_ROWID,KEY_LABEL},
              KEY_ROWID + " != " + getArguments().getLong("fromAccountId"), null, null), -1, KEY_LABEL, this)
      .create();
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    dismiss();
    Transaction.move(
        getArguments().getLong("transactionId"),
        ((AlertDialog) dialog).getListView().getItemIdAtPosition(which));
/*    ((MessageDialogListener) getActivity())
      .dispatchCommand(R.id.MOVE_TRANSACTION_COMMAND,
          ((AlertDialog) dialog).getListView().getItemIdAtPosition(which));*/
  }
}