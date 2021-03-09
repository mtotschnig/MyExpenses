package org.totschnig.myexpenses.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.util.ImportFileResultHandler;
import org.totschnig.myexpenses.util.PermissionHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import static org.totschnig.myexpenses.activity.ConstantsKt.IMPORT_FILENAME_REQUEST_CODE;

public abstract class ImportSourceDialogFragment extends BaseDialogFragment
    implements OnClickListener, DialogInterface.OnClickListener, ImportFileResultHandler.FileNameHostFragment {

  protected EditText mFilename;

  public Uri getUri() {
    return mUri;
  }

  @Override
  public void setUri(@Nullable Uri uri) {
    mUri = uri;
  }

  @Override
  public EditText getFilenameEditText() {
    return mFilename;
  }

  protected Uri mUri;

  public ImportSourceDialogFragment() {
    super();
  }
  abstract int getLayoutId();
  abstract String getLayoutTitle();
  public boolean checkTypeParts(String[] typeParts, String extension) {
   return ImportFileResultHandler.checkTypePartsDefault(typeParts);
  }

  @Override
  public void onCancel (DialogInterface dialog) {
    if (getActivity()==null) {
      return;
    }
    //TODO: we should not depend on 
    ((MessageDialogListener) getActivity()).onMessageDialogDismissOrCancel();
  }
  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = initBuilderWithView(getLayoutId());
    setupDialogView(dialogView);
    return builder.setTitle(getLayoutTitle())
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
    mFilename = view.findViewById(R.id.Filename);

    view.findViewById(R.id.btn_browse).setOnClickListener(this);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == IMPORT_FILENAME_REQUEST_CODE) {
      if (resultCode == Activity.RESULT_OK && data != null) {
        try {
          mUri = ImportFileResultHandler.handleFilenameRequestResult(this, data);
        } catch (Throwable throwable) {
          mUri = null;
          showSnackbar(throwable.getMessage(), Snackbar.LENGTH_LONG, null);
        }
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
    ImportFileResultHandler.handleFileNameHostOnResume(this, prefHandler);
    setButtonState();
  }

  //we cannot persist document Uris because we use ACTION_GET_CONTENT instead of ACTION_OPEN_DOCUMENT
  protected void maybePersistUri() {
    ImportFileResultHandler.maybePersistUri(this, prefHandler);
  }

  @Override
  public void onClick(View v) {
   DialogUtils.openBrowse(mUri, this);
  }

  protected boolean isReady() {
    return mUri != null && PermissionHelper.canReadUri(mUri, getContext());
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