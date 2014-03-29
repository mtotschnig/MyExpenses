package org.totschnig.myexpenses.dialog;

import org.totschnig.myexpenses.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;

public class QifImportDialogFragment extends DialogFragment implements
DialogInterface.OnClickListener {
  public static final QifImportDialogFragment newInstance() {
    return new QifImportDialogFragment();
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Context wrappedCtx = DialogUtils.wrapContext2(getActivity());
    LayoutInflater li = LayoutInflater.from(wrappedCtx);
    View view = li.inflate(R.layout.qif_import_dialog, null);
    return new AlertDialog.Builder(wrappedCtx)
      .setTitle(R.string.dialog_title_select_import_source)
      .setView(view)
      .setPositiveButton(android.R.string.ok,this)
      .setNegativeButton(android.R.string.cancel,null)
      .create();
  }

  @Override
  public void onCancel (DialogInterface dialog) {
    getActivity().finish();
  }
  @Override
  public void onClick(DialogInterface dialog, int id) {
    switch (id) {
    case AlertDialog.BUTTON_POSITIVE:
     
      break;
    case AlertDialog.BUTTON_NEUTRAL:
      
      break;
    case AlertDialog.BUTTON_NEGATIVE:
      onCancel(dialog);
      break;
    default:
    }
  }
}
