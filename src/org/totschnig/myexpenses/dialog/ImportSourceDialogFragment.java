package org.totschnig.myexpenses.dialog;

import java.util.List;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
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
    implements OnClickListener, DialogInterface.OnClickListener  {

  public static final int IMPORT_FILENAME_REQUESTCODE = 1;
  protected EditText mFilename;
  protected Uri mUri;
  final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

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
    // TODO Auto-generated method stub
    super.onDestroyView();
    mFilename = null;
  }

  protected void setupDialogView(View view) {
    mFilename = (EditText) view.findViewById(R.id.Filename);
    mFilename.setEnabled(false);

    view.findViewById(R.id.btn_browse).setOnClickListener(this);
  }

  public void openBrowse() {
  
    Intent intent = new Intent(Utils.getContentIntent());
    intent.addCategory(Intent.CATEGORY_OPENABLE);
  
    intent.setDataAndType(mUri,getMimeType());
    
    if (isKitKat && !Utils.isIntentAvailable(getActivity(), intent)) {
      //fallback
      intent = new Intent(Intent.ACTION_GET_CONTENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      intent.setDataAndType(mUri,getMimeType());
    }
  
    try {
        startActivityForResult(intent, IMPORT_FILENAME_REQUESTCODE);
    } catch (ActivityNotFoundException e) {
        // No compatible file manager was found.
        Toast.makeText(getActivity(), R.string.no_filemanager_installed, Toast.LENGTH_SHORT).show();
    } catch(SecurityException ex) {
      Toast.makeText(getActivity(),
          String.format(
              "Sorry, this destination does not accept %s request. Please select a different one.",intent.getAction()),
          Toast.LENGTH_SHORT)
        .show();
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
          String displayName = getDisplayName(mUri);
          mFilename.setText(displayName);
          if (displayName == null) {
            mUri = null;
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
            try {
              getActivity().getContentResolver().takePersistableUriPermission(mUri, takeFlags);
            } catch (SecurityException e) {
              Utils.reportToAcra(e);
            }
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
  public static String getDisplayName(Uri uri) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      // The query, since it only applies to a single document, will only return
      // one row. There's no need to filter, sort, or select fields, since we want
      // all fields for one document.
      try {
        Cursor cursor = MyApplication.getInstance().getContentResolver()
                .query(uri, null, null, null, null, null);
  
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
      } catch (Exception e) {
        Utils.reportToAcra(e);
        return null;
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
        String displayName = getDisplayName(restoredUri);
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
   openBrowse();
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
        String displayName = getDisplayName(restoredUri);
        if (displayName != null) {
          mUri = restoredUri;
          mFilename.setText(displayName);
        }
      }
    }
  }
}