package org.totschnig.myexpenses.dialog;

import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.GrisbiImport;
import org.totschnig.myexpenses.activity.QifImport;
import org.totschnig.myexpenses.export.qif.QifDateFormat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class GrisbiSourcesDialogFragment extends DialogFragment implements
DialogInterface.OnClickListener {
  private EditText mFilename;
  private AlertDialog mDialog;
  
  public static final GrisbiSourcesDialogFragment newInstance() {
    return new GrisbiSourcesDialogFragment();
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Context wrappedCtx = DialogUtils.wrapContext2(getActivity());
    LayoutInflater li = LayoutInflater.from(wrappedCtx);
    View view = li.inflate(R.layout.grisbi_import_dialog, null);
    mFilename = (EditText) view.findViewById(R.id.Filename);
    mFilename.addTextChangedListener(new TextWatcher(){
      public void afterTextChanged(Editable s) {
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(
            !TextUtils.isEmpty(s.toString()));
      }
      public void beforeTextChanged(CharSequence s, int start, int count, int after){}
      public void onTextChanged(CharSequence s, int start, int before, int count){}
    });
    mDialog = new AlertDialog.Builder(wrappedCtx)
      .setTitle(R.string.pref_import_from_grisbi_title)
      .setView(view)
      .setPositiveButton(android.R.string.ok,this)
      .setNegativeButton(android.R.string.cancel, this)
      .create();
    return mDialog;
  }

  @Override
  public void onStart(){
    super.onStart();
    mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(
        !TextUtils.isEmpty(mFilename.getText().toString()));
  }

  @Override
  public void onCancel (DialogInterface dialog) {
    ((GrisbiImport) getActivity()).onMessageDialogDismissOrCancel();
  }
  @Override
  public void onClick(DialogInterface dialog, int id) {
    switch (id) {
    case AlertDialog.BUTTON_POSITIVE:
      ((GrisbiImport) getActivity()).onSourceSelected(
          mFilename.getText().toString()
          );
    case AlertDialog.BUTTON_NEGATIVE:
      onCancel(dialog);
      break;
    }
  }
}
