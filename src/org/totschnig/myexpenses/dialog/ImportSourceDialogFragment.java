package org.totschnig.myexpenses.dialog;

import java.util.List;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivityNoAppCompat;
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
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

public abstract class ImportSourceDialogFragment extends DialogFragment
    implements OnClickListener, DialogInterface.OnClickListener, OnCheckedChangeListener  {

  public static final int IMPORT_FILENAME_REQUESTCODE = 1;
  protected EditText mFilename;
  protected Uri mUri;
  protected AlertDialog mDialog;
  protected CheckBox mImportCategories;
  protected CheckBox mImportParties;
  protected CheckBox mImportTransactions;
  protected Context wrappedCtx;
  final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
  final boolean isJellyBean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;

  public ImportSourceDialogFragment() {
    super();
  }
  abstract int getLayoutId();
  abstract int getLayoutTitle();
  abstract String getTypeName();
  abstract String getPrefKey();
  protected String getMimeType() {
    return "*/*";
  }
  protected boolean checkTypeParts(String[] typeParts) {
    return typeParts[0].equals("*") || 
    typeParts[0].equals("text") || 
    typeParts[0].equals("application");
  }

  @Override
  public void onCancel (DialogInterface dialog) {
    ((ProtectedFragmentActivityNoAppCompat) getActivity()).onMessageDialogDismissOrCancel();
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    wrappedCtx = DialogUtils.wrapContext2(getActivity());
    LayoutInflater li = LayoutInflater.from(wrappedCtx);
    View view = li.inflate(getLayoutId(), null);
    setupDialogView(view);
    mDialog = new AlertDialog.Builder(wrappedCtx)
      .setTitle(getLayoutTitle())
      .setView(view)
      .setPositiveButton(android.R.string.ok,this)
      .setNegativeButton(android.R.string.cancel,this)
      .create();
    return mDialog;
  }

  protected void setupDialogView(View view) {
    mFilename = (EditText) view.findViewById(R.id.Filename);
    mFilename.setEnabled(false);

    view.findViewById(R.id.btn_browse).setOnClickListener(this);
    mImportCategories = (CheckBox) view.findViewById(R.id.import_select_categories);
    if (mImportCategories != null) {
      mImportCategories.setOnCheckedChangeListener(this);
      mImportParties = (CheckBox) view.findViewById(R.id.import_select_parties);
      mImportParties.setOnCheckedChangeListener(this);
      mImportTransactions = (CheckBox) view.findViewById(R.id.import_select_transactions);
      mImportTransactions.setOnCheckedChangeListener(this);
    }
  }

  @SuppressLint("InlinedApi")
  public void openBrowse() {
  
    Intent intent = new Intent(isKitKat ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
  
    intent.setDataAndType(mUri,getMimeType());
  
    try {
        startActivityForResult(intent, IMPORT_FILENAME_REQUESTCODE);
    } catch (ActivityNotFoundException e) {
        // No compatible file manager was found.
        Toast.makeText(getActivity(), R.string.no_filemanager_installed, Toast.LENGTH_SHORT).show();
    }
  }

  @SuppressLint("NewApi")
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == IMPORT_FILENAME_REQUESTCODE) {
      if (resultCode == Activity.RESULT_OK && data != null) {
        mUri = data.getData();
        if (mUri != null) {
          mFilename.setError(null);
          mFilename.setText(getDisplayName());
          if (mUri == null) {
            //SecurityException raised during getDisplayName
            mFilename.setError("Error while retrieving document");
          } else {
            String type = getActivity().getContentResolver().getType(mUri);
            if (type != null) {
              String[] typeParts = type.split("/");
              if (typeParts.length==0 ||
                  !checkTypeParts(typeParts)) {
                mUri = null;
                mFilename.setError(getString(R.string.import_source_select_error,getTypeName()));
              }
            }
          }
          if (isKitKat && mUri != null) {
            final int takeFlags = data.getFlags()
                & Intent.FLAG_GRANT_READ_URI_PERMISSION;
            getActivity().getContentResolver().takePersistableUriPermission(mUri, takeFlags);
          }
        }
      }
    }
  }
  //https://developer.android.com/guide/topics/providers/document-provider.html
  /**
   * @return display name for document stored at mUri.
   * Returns null if accessing mUri raises {@link SecurityException}
   */
  @SuppressLint("NewApi")
  protected String getDisplayName() {

    if (isJellyBean) {
      // The query, since it only applies to a single document, will only return
      // one row. There's no need to filter, sort, or select fields, since we want
      // all fields for one document.
      try {
        Cursor cursor = getActivity().getContentResolver()
                .query(mUri, null, null, null, null, null);
  
        if (cursor != null) {
          try {
            if (cursor.moveToFirst()) {
              // Note it's called "Display Name".  This is
              // provider-specific, and might not necessarily be the file name.
              int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
              if (columnIndex != -1) {
                return cursor.getString(columnIndex);
              }
            }
          } catch (Exception e) {}
            finally {
            cursor.close();
          }
        }
      } catch (SecurityException e) {
        mUri = null;
        return null;
      }
    }
    List<String> filePathSegments = mUri.getPathSegments();
    if (filePathSegments.size()>0) {
      return filePathSegments.get(filePathSegments.size()-1);
    } else {
      return "UNKNOWN";
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    setButtonState();
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
      String storedUri = MyApplication.getInstance().getSettings()
          .getString(getPrefKey(), "");
      if (!storedUri.equals("")) {
        mUri = Uri.parse(storedUri);
        mFilename.setText(getDisplayName());
      }
    }
    setButtonState();
  }
  @Override
  public void onClick(View v) {
   openBrowse();
  }
  private void setButtonState() {
    boolean isReady = false;
    if (mUri != null) {
      if (mImportCategories != null) {
        isReady = (mImportCategories.getVisibility() == View.VISIBLE && mImportCategories.isChecked()) ||
            (mImportParties.getVisibility() == View.VISIBLE && mImportParties.isChecked()) ||
            (mImportTransactions.getVisibility() == View.VISIBLE && mImportTransactions.isChecked());
      } else {
        isReady = true;
      }
    }
    mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isReady);
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
      String restoredUri = savedInstanceState.getString(getPrefKey());
      if (restoredUri != null) {
        mUri = Uri.parse(restoredUri);
        mFilename.setText(getDisplayName());
      }
    }
  }
}