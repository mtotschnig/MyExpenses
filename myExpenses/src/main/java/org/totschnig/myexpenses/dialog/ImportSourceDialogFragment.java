package org.totschnig.myexpenses.dialog;

import java.util.List;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.util.Utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

public abstract class ImportSourceDialogFragment extends CommitSafeDialogFragment
    implements OnClickListener, DialogInterface.OnClickListener, DialogUtils.UriTypePartChecker  {

  protected EditText mFilename;
  protected Uri mUri;

  public ImportSourceDialogFragment() {
    super();
  }
  abstract int getLayoutId();
  abstract String getLayoutTitle();
  abstract String getTypeName();
  abstract String getPrefKey();
  public boolean checkTypeParts(String[] typeParts) {
   return DialogUtils.checkTypePartsDefault(typeParts);
  }

  @Override
  public void onCancel (DialogInterface dialog) {
    if (getActivity()==null) {
      return;
    }
    //TODO: we should not depend on 
    ((MessageDialogListener) getActivity()).onMessageDialogDismissOrCancel();
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Context wrappedCtx = DialogUtils.wrapContext2(getActivity());
    LayoutInflater li = LayoutInflater.from(wrappedCtx);
    View view = li.inflate(getLayoutId(), null);
    setupDialogView(view);
    return new AlertDialog.Builder(wrappedCtx)
      .setTitle(getLayoutTitle())
      .setView(view)
      .setPositiveButton(android.R.string.ok,this)
      .setNegativeButton(android.R.string.cancel,this)
      .create();
  }
  @Override
  public void onDestroyView() {
    super.onDestroyView();
    mFilename = null;
  }

  protected void setupDialogView(View view) {
    mFilename = DialogUtils.configureFilename(view);

    view.findViewById(R.id.btn_browse).setOnClickListener(this);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == ProtectedFragmentActivity.IMPORT_FILENAME_REQUESTCODE) {
      if (resultCode == Activity.RESULT_OK && data != null) {
        mUri = DialogUtils.handleFilenameRequestResult(data, mFilename, getTypeName(), this);
      }
    }
  }


  @Override
  public void onClick(DialogInterface dialog, int id) {
    if (id == AlertDialog.BUTTON_NEGATIVE) {
      onCancel(dialog);
    }
  }
  @Override
  public void onResume() {
    super.onResume();
    if (mUri==null) {
      String restoredUriString = MyApplication.getInstance().getSettings()
          .getString(getPrefKey(), "");
      if (!restoredUriString.equals("")) {
        Uri restoredUri = Uri.parse(restoredUriString);
        String displayName = DialogUtils.getDisplayName(restoredUri);
        if (displayName != null) {
          mUri = restoredUri;
          mFilename.setText(displayName);
        }
      }
    }
    setButtonState();
  }
  @Override
  public void onClick(View v) {
   DialogUtils.openBrowse(mUri,this);
  }
  protected boolean isReady() {
    return mUri != null;
  }
  protected void setButtonState() {
    ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isReady());
  }
  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (mUri != null) {
      outState.putString(getPrefKey(), mUri.toString());
    }
  }
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (savedInstanceState != null) {
      String restoredUriString = savedInstanceState.getString(getPrefKey());
      if (restoredUriString != null) {
        Uri restoredUri = Uri.parse(restoredUriString);
        String displayName = DialogUtils.getDisplayName(restoredUri);
        if (displayName != null) {
          mUri = restoredUri;
          mFilename.setText(displayName);
        }
      }
    }
  }
}