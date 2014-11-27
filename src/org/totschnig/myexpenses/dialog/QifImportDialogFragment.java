package org.totschnig.myexpenses.dialog;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;

import java.util.Arrays;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.QifImport;
import org.totschnig.myexpenses.export.qif.QifDateFormat;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class QifImportDialogFragment extends TextSourceDialogFragment implements
    LoaderManager.LoaderCallbacks<Cursor>, OnItemSelectedListener {
  Spinner mAccountSpinner, mDateFormatSpinner, mCurrencySpinner, mEncodingSpinner;
  private SimpleCursorAdapter mAccountsAdapter;

  static final String PREFKEY_IMPORT_QIF_DATE_FORMAT = "import_qif_date_format";
  static final String PREFKEY_IMPORT_QIF_ENCODING = "import_qif_encoding";
  private MergeCursor mAccountsCursor;

  public static final QifImportDialogFragment newInstance() {
    return new QifImportDialogFragment();
  }
  @Override
  protected int getLayoutId() {
    return R.layout.qif_import_dialog;
  }
  @Override
  protected int getLayoutTitle() {
    return R.string.pref_import_qif_title;
  }

  @Override
  String getTypeName() {
    return "QIF";
  }

  @Override
  String getPrefKey() {
    return "import_qif_file_uri";
  }
  @Override
  public void onClick(DialogInterface dialog, int id) {
    if (getActivity()==null) {
      return;
    }
    if (id == AlertDialog.BUTTON_POSITIVE) {
      QifDateFormat format = (QifDateFormat) mDateFormatSpinner.getSelectedItem();
      String encoding = (String) mEncodingSpinner.getSelectedItem();
      SharedPreferencesCompat.apply(
        MyApplication.getInstance().getSettings().edit()
          .putString(getPrefKey(), mUri.toString())
          .putString(PREFKEY_IMPORT_QIF_ENCODING, encoding)
          .putString(PREFKEY_IMPORT_QIF_DATE_FORMAT, format.name()));
      ((QifImport) getActivity()).onSourceSelected(
          mUri,
          format,
          mAccountSpinner.getSelectedItemId(),
          ((Account.CurrencyEnum) mCurrencySpinner.getSelectedItem()).name(),
          mImportTransactions.isChecked(),
          mImportCategories.isChecked(),
          mImportParties.isChecked(),
          encoding
          );
    } else {
      super.onClick(dialog, id);
    }
  }
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    if (getActivity()==null) {
      return null;
    }
    CursorLoader cursorLoader = new CursorLoader(
        getActivity(),
        TransactionProvider.ACCOUNTS_BASE_URI,
        new String[] {
          KEY_ROWID,
          KEY_LABEL,
          KEY_CURRENCY},
        null,null, null);
    return cursorLoader;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    MatrixCursor extras = new MatrixCursor(new String[] {
        KEY_ROWID,
        KEY_LABEL,
        KEY_CURRENCY
    });
    extras.addRow(new String[] {
        "0",
        getString(R.string.menu_create_account),
        Account.getLocaleCurrency().getCurrencyCode()
    });
    mAccountsCursor = new MergeCursor(new Cursor[] {extras,data});
    mAccountsAdapter.swapCursor(mAccountsCursor);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    mAccountsCursor = null;
    mAccountsAdapter.swapCursor(null);
  }

  @Override
  protected void setupDialogView(View view) {
    super.setupDialogView(view);
    mAccountSpinner = (Spinner) view.findViewById(R.id.Account);
    mAccountsAdapter = new SimpleCursorAdapter(wrappedCtx, android.R.layout.simple_spinner_item, null,
        new String[] {KEY_LABEL}, new int[] {android.R.id.text1}, 0);
    mAccountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mAccountSpinner.setAdapter(mAccountsAdapter);
    mAccountSpinner.setOnItemSelectedListener(this);
    getLoaderManager().initLoader(0, null, this);

    mDateFormatSpinner = (Spinner) view.findViewById(R.id.DateFormat);
    ArrayAdapter<QifDateFormat> dateFormatAdapter =
        new ArrayAdapter<QifDateFormat>(
            wrappedCtx, android.R.layout.simple_spinner_item, QifDateFormat.values());
    dateFormatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mDateFormatSpinner.setAdapter(dateFormatAdapter);
    QifDateFormat qdf;
    try {
      qdf = QifDateFormat.valueOf(
          MyApplication.getInstance().getSettings()
          .getString(PREFKEY_IMPORT_QIF_DATE_FORMAT, "EU"));
    } catch (IllegalArgumentException e) {
      qdf = QifDateFormat.EU;
    }
    mDateFormatSpinner.setSelection(qdf.ordinal());


    mEncodingSpinner = (Spinner) view.findViewById(R.id.Encoding);
    mEncodingSpinner.setSelection(
        Arrays.asList(getResources().getStringArray(R.array.pref_qif_export_file_encoding))
        .indexOf(MyApplication.getInstance().getSettings()
            .getString(PREFKEY_IMPORT_QIF_ENCODING, "UTF-8")));

    mCurrencySpinner = (Spinner) view.findViewById(R.id.Currency);
    ArrayAdapter<Account.CurrencyEnum> curAdapter = new ArrayAdapter<Account.CurrencyEnum>(
        wrappedCtx, android.R.layout.simple_spinner_item, android.R.id.text1,Account.CurrencyEnum.values());
    curAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mCurrencySpinner.setAdapter(curAdapter);
//    mCurrencySpinner.setSelection(
//        Account.CurrencyEnum
//        .valueOf(Account.getLocaleCurrency().getCurrencyCode())
//        .ordinal());
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position,
      long id) {
    if (mAccountsCursor != null) {
      mAccountsCursor.moveToPosition(position);
      mCurrencySpinner.setSelection(
          Account.CurrencyEnum
          .valueOf(
              mAccountsCursor.getString(2))//2=KEY_CURRENCY
          .ordinal());
      mCurrencySpinner.setEnabled(position==0);
    }
  }
  @Override
  public void onNothingSelected(AdapterView<?> parent) {
  }
}
