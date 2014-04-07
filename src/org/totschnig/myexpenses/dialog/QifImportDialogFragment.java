package org.totschnig.myexpenses.dialog;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.GrisbiImport;
import org.totschnig.myexpenses.activity.QifImport;
import org.totschnig.myexpenses.export.qif.QifDateFormat;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class QifImportDialogFragment extends DialogFragment implements
DialogInterface.OnClickListener, OnClickListener, LoaderManager.LoaderCallbacks<Cursor> {
  private EditText mFilename;
  private Spinner mAccountSpinner, mDateFormatSpinner;
  private SimpleCursorAdapter mAccountsAdapter;
  private AlertDialog mDialog;
  public static final QifImportDialogFragment newInstance() {
    return new QifImportDialogFragment();
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Context wrappedCtx = DialogUtils.wrapContext2(getActivity());
    LayoutInflater li = LayoutInflater.from(wrappedCtx);
    View view = li.inflate(R.layout.qif_import_dialog, null);
    mFilename = (EditText) view.findViewById(R.id.Filename);
    mFilename.addTextChangedListener(new TextWatcher(){
      public void afterTextChanged(Editable s) {
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(
            !TextUtils.isEmpty(s.toString()));
      }
      public void beforeTextChanged(CharSequence s, int start, int count, int after){}
      public void onTextChanged(CharSequence s, int start, int before, int count){}
    });
    mAccountSpinner = (Spinner) view.findViewById(R.id.Account);
    mDateFormatSpinner = (Spinner) view.findViewById(R.id.DateFormat);
    ArrayAdapter<QifDateFormat> dateFormatAdapter =
        new ArrayAdapter<QifDateFormat>(
            getActivity(), android.R.layout.simple_spinner_item, QifDateFormat.values());
    dateFormatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mDateFormatSpinner.setAdapter(dateFormatAdapter);
    mAccountsAdapter = new SimpleCursorAdapter(getActivity(), android.R.layout.simple_spinner_item, null,
        new String[] {KEY_LABEL}, new int[] {android.R.id.text1}, 0);
    mAccountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mAccountSpinner.setAdapter(mAccountsAdapter);
    view.findViewById(R.id.btn_browse).setOnClickListener(this);
    getLoaderManager().initLoader(0, null, this);
    mDialog = new AlertDialog.Builder(wrappedCtx)
      .setTitle(R.string.dialog_title_select_import_source)
      .setView(view)
      .setPositiveButton(android.R.string.ok,this)
      .setNegativeButton(android.R.string.cancel,this)
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
    getActivity().finish();
  }

  @Override
  public void onClick(DialogInterface dialog, int id) {
    switch (id) {
    case AlertDialog.BUTTON_POSITIVE:
      ((QifImport) getActivity()).onSourceSelected(
          mFilename.getText().toString(),
          (QifDateFormat) mDateFormatSpinner.getSelectedItem(),
          mAccountSpinner.getSelectedItemId()
          );
      break;
    case AlertDialog.BUTTON_NEGATIVE:
      onCancel(dialog);
      break;
    default:
    }
  }
  public void openBrowse() {
    String filePath = mFilename.getText().toString();

    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);

    File file = new File(filePath);
    intent.setDataAndType(Uri.fromFile(file),"*/*");

    try {
        startActivityForResult(intent, QifImport.IMPORT_FILENAME_REQUESTCODE);
    } catch (ActivityNotFoundException e) {
        // No compatible file manager was found.
        Toast.makeText(getActivity(), R.string.no_filemanager_installed, Toast.LENGTH_SHORT).show();
    }
  }
  @Override
  public void onActivityResult(int requestCode, int resultCode,
      Intent data) {
    if (requestCode == QifImport.IMPORT_FILENAME_REQUESTCODE) {
      if (resultCode == Activity.RESULT_OK && data != null) {
          Uri fileUri = data.getData();
          if (fileUri != null) {
              String filePath = fileUri.getPath();
              if (filePath != null) {
                  mFilename.setText(filePath);
                  //savePreferences();
              }
          }
      }
    }
  }

  @Override
  public void onClick(View v) {
   openBrowse();
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    CursorLoader cursorLoader = new CursorLoader(
        getActivity(),
        TransactionProvider.ACCOUNTS_BASE_URI,
        new String[] {KEY_ROWID,KEY_LABEL},
        null,null, null);
    return cursorLoader;

  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    MatrixCursor extras = new MatrixCursor(new String[] { KEY_ROWID,KEY_LABEL });
    extras.addRow(new String[] { "0", getString(R.string.menu_create_account)});
    mAccountsAdapter.swapCursor(new MergeCursor(new Cursor[] {extras,data}));
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    mAccountsAdapter.swapCursor(null);
  }
}
