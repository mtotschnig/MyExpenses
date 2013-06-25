package org.totschnig.myexpenses.dialog;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

import org.totschnig.myexpenses.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Could be generalized to handle any uri and a generic callback interface like MessageDialogFragment
 *
 */
public class SelectFromUriDialogFragment extends DialogFragment implements OnClickListener {
  public interface SelectFromUriDialogListener {
    void onItemSelected(Bundle args);
}
  /**
   * @param args required keys: uri,selection, column
   * @return
   */
  public static final SelectFromUriDialogFragment newInstance(Bundle args) {
    SelectFromUriDialogFragment dialogFragment = new SelectFromUriDialogFragment();
    dialogFragment.setArguments(args);
    return dialogFragment;
  }
  //KEY_ROWID + " != " + getArguments().getLong("fromAccountId")
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Context ctx = getActivity();
    Bundle bundle = getArguments();
    String column = bundle.getString("column");
    return new AlertDialog.Builder(ctx)
      .setTitle(bundle.getString("dialogTitle"))
      .setSingleChoiceItems(
          ctx.getContentResolver().query(
              (Uri) bundle.getParcelable("uri"),
              new String[] {KEY_ROWID,column},
              bundle.getString("selection"), null, null), -1, column, this)
      .create();
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    SelectFromUriDialogListener activity = (SelectFromUriDialogListener) getActivity();
    Bundle args = getArguments();
    args.putLong("result", ((AlertDialog) dialog).getListView().getItemIdAtPosition(which));
    activity.onItemSelected(args);
    dismiss();
/*    ((MessageDialogListener) getActivity())
      .dispatchCommand(R.id.MOVE_TRANSACTION_COMMAND,
          ((AlertDialog) dialog).getListView().getItemIdAtPosition(which));*/
  }
}