package org.totschnig.myexpenses.dialog;

import java.util.List;

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
import android.text.Editable;
import android.text.TextWatcher;
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
    mFilename.addTextChangedListener(new TextWatcher(){
      public void afterTextChanged(Editable s) {
        mFilename.setError(null);
        setButtonState();
      }
      public void beforeTextChanged(CharSequence s, int start, int count, int after){}
      public void onTextChanged(CharSequence s, int start, int before, int count){}
    });
    if (isKitKat) {
      //on Kitkat user needs to allow us explicitly to open a file through selecting it
      mFilename.setEnabled(false);
    }
    view.findViewById(R.id.btn_browse).setOnClickListener(this);
    mImportCategories = (CheckBox) view.findViewById(R.id.import_select_categories);
    mImportCategories.setOnCheckedChangeListener(this);
    mImportParties = (CheckBox) view.findViewById(R.id.import_select_parties);
    mImportParties.setOnCheckedChangeListener(this);
    mImportTransactions = (CheckBox) view.findViewById(R.id.import_select_transactions);
    mImportTransactions.setOnCheckedChangeListener(this);
  }

  @SuppressLint("InlinedApi")
  public void openBrowse() {
  
    Intent intent = new Intent(isKitKat ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
  
    intent.setDataAndType(mUri,"*/*");
  
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
          String[] typeParts = getActivity().getContentResolver().getType(mUri).split("/");
          mFilename.setText(getDisplayName(mUri));
          if (typeParts.length==0 ||
              !(
                  typeParts[0].equals("*") || 
                  typeParts[0].equals("text") || 
                  typeParts[0].equals("application")
              )) {
            mUri = null;
            mFilename.setError("Please provide a file of type X");
          }
        }
      }
    }
  }
  //https://developer.android.com/guide/topics/providers/document-provider.html
  @SuppressLint("NewApi")
  protected String getDisplayName(Uri uri) {

    if (isJellyBean) {
      // The query, since it only applies to a single document, will only return
      // one row. There's no need to filter, sort, or select fields, since we want
      // all fields for one document.
      Cursor cursor = getActivity().getContentResolver()
              .query(uri, null, null, null, null, null);
  
      try {
      // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
      // "if there's anything to look at, look at it" conditionals.
          if (cursor != null && cursor.moveToFirst()) {
              // Note it's called "Display Name".  This is
              // provider-specific, and might not necessarily be the file name.
              return cursor.getString(
                      cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
          }
      } finally {
          cursor.close();
      }
    }
    List<String> filePathSegments = uri.getPathSegments();
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
  public void onStart(){
    super.onStart();
    setButtonState();
  }
  @Override
  public void onClick(View v) {
   openBrowse();
  }
  private void setButtonState() {
    boolean isReady = false;
    if (mUri != null) {
      isReady = (mImportCategories.getVisibility() == View.VISIBLE && mImportCategories.isChecked()) ||
          (mImportParties.getVisibility() == View.VISIBLE && mImportParties.isChecked()) ||
          (mImportTransactions.getVisibility() == View.VISIBLE && mImportTransactions.isChecked());
    }
    mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isReady);
  }
}