package org.totschnig.myexpenses.dialog;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class SelectFromCursorDialogFragment extends DialogFragment implements OnClickListener {
  public interface SelectFromCursorDialogListener {
    Cursor getCursor(int cursorId);
    void onItemSelected(Bundle args);
}
  /**
   * @param args required keys: uri,selection, column
   * @return
   */
  public static final SelectFromCursorDialogFragment newInstance(Bundle args) {
    SelectFromCursorDialogFragment dialogFragment = new SelectFromCursorDialogFragment();
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
          ((SelectFromCursorDialogListener) ctx).getCursor(bundle.getInt("cursorId"))
          , -1, column, this)
      .create();
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    SelectFromCursorDialogListener activity = (SelectFromCursorDialogListener) getActivity();
    Bundle args = getArguments();
    args.putLong("result", ((AlertDialog) dialog).getListView().getItemIdAtPosition(which));
    activity.onItemSelected(args);
    dismiss();
  }
}