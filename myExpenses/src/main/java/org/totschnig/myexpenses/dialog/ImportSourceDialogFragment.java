package org.totschnig.myexpenses.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;
import org.totschnig.myexpenses.util.FileUtils;

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
    LayoutInflater li = LayoutInflater.from(getActivity());
    View view = li.inflate(getLayoutId(), null);
    setupDialogView(view);
    return new AlertDialog.Builder(getActivity())
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
        if (!FileUtils.isDocumentUri(getActivity(),restoredUri)) {
          String displayName = DialogUtils.getDisplayName(restoredUri);
          if (displayName != null) {
            mUri = restoredUri;
            mFilename.setText(displayName);
          }
        }
      }
    }
    setButtonState();
  }

  //we cannot persist document Uris because we use ACTION_GET_CONTENT instead of ACTION_OPEN_DOCUMENT
  protected void maybePersistUri() {
    if (!FileUtils.isDocumentUri(getActivity(),mUri)) {
      SharedPreferencesCompat.apply(
          MyApplication.getInstance().getSettings().edit()
              .putString(getPrefKey(), mUri.toString()));
    }
  }

  @Override
  public void onClick(View v) {
   DialogUtils.openBrowse(mUri, this);
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