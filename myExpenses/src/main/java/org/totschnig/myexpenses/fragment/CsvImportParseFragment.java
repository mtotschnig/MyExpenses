package org.totschnig.myexpenses.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import com.google.android.material.snackbar.Snackbar;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.adapter.CurrencyAdapter;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.export.qif.QifDateFormat;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.ImportFileResultHandler;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel;
import org.totschnig.myexpenses.viewmodel.data.Currency;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import static org.totschnig.myexpenses.activity.ProtectedFragmentActivity.ASYNC_TAG;
import static org.totschnig.myexpenses.activity.ProtectedFragmentActivity.PROGRESS_TAG;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;

public class CsvImportParseFragment extends Fragment implements View.OnClickListener,
    LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemSelectedListener, ImportFileResultHandler.FileNameHostFragment {
  static final String PREFKEY_IMPORT_CSV_DATE_FORMAT = "import_csv_date_format";
  static final String PREFKEY_IMPORT_CSV_ENCODING = "import_csv_encoding";
  static final String PREFKEY_IMPORT_CSV_DELIMITER = "import_csv_delimiter";
  private Uri mUri;
  private CurrencyViewModel currencyViewModel;

  @Override
  public String getPrefKey() {
    return "import_csv_file_uri";
  }

  @Override
  public Uri getUri() {
    return mUri;
  }

  @Override
  public void setUri(Uri mUri) {
    this.mUri = mUri;
    getActivity().supportInvalidateOptionsMenu();
  }

  @Override
  public EditText getFilenameEditText() {
    return mFilename;
  }

  private EditText mFilename;
  private Spinner mDateFormatSpinner, mEncodingSpinner, mDelimiterSpinner, mAccountSpinner,
      mCurrencySpinner, mTypeSpinner;
  private MergeCursor mAccountsCursor;
  private SimpleCursorAdapter mAccountsAdapter;
  private long accountId = 0;
  private String currency = null;
  private AccountType type = null;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    if (savedInstanceState != null) {
      accountId = savedInstanceState.getLong(KEY_ACCOUNTID);
      currency = savedInstanceState.getString(KEY_CURRENCY);
      type = (AccountType) savedInstanceState.getSerializable(KEY_TYPE);
    }

    View view = inflater.inflate(R.layout.import_csv_parse, container, false);
    mDateFormatSpinner = DialogUtils.configureDateFormat(view, getActivity(), PREFKEY_IMPORT_CSV_DATE_FORMAT);
    mEncodingSpinner = DialogUtils.configureEncoding(view, getActivity(), PREFKEY_IMPORT_CSV_ENCODING);
    mDelimiterSpinner = DialogUtils.configureDelimiter(view, getActivity(), PREFKEY_IMPORT_CSV_DELIMITER);
    mFilename = DialogUtils.configureFilename(view);
    mAccountSpinner = view.findViewById(R.id.Account);
    Context wrappedCtx = view.getContext();
    mAccountsAdapter = new SimpleCursorAdapter(wrappedCtx, android.R.layout.simple_spinner_item, null,
        new String[]{KEY_LABEL}, new int[]{android.R.id.text1}, 0);
    mAccountsAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
    mAccountSpinner.setAdapter(mAccountsAdapter);
    mAccountSpinner.setOnItemSelectedListener(this);
    mCurrencySpinner = DialogUtils.configureCurrencySpinner(view, this);
    currencyViewModel.getCurrencies().observe(getViewLifecycleOwner(), currencies -> {
      final CurrencyAdapter adapter = (CurrencyAdapter) mCurrencySpinner.getAdapter();
      adapter.addAll(currencies);
      mCurrencySpinner.setSelection(adapter.getPosition(currencyViewModel.getDefault()));
    });
    currencyViewModel.loadCurrencies();
    mTypeSpinner = DialogUtils.configureTypeSpinner(view);
    mTypeSpinner.setOnItemSelectedListener(this);
    getLoaderManager().initLoader(0, null, this);
    view.findViewById(R.id.btn_browse).setOnClickListener(this);
    return view;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    currencyViewModel = ViewModelProviders.of(this).get(CurrencyViewModel.class);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == ProtectedFragmentActivity.IMPORT_FILENAME_REQUESTCODE) {
      if (resultCode == Activity.RESULT_OK && data != null) {
        try {
          setUri(ImportFileResultHandler.handleFilenameRequestResult(this, data));
        } catch (Throwable throwable) {
          setUri(null);
          ((ProtectedFragmentActivity) getActivity()).showSnackbar(throwable.getMessage(), Snackbar.LENGTH_LONG);
        }
      }
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    ImportFileResultHandler.handleFileNameHostOnResume(this);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (mUri != null) {
      outState.putString(getPrefKey(), mUri.toString());
    }
    outState.putLong(KEY_ACCOUNTID, accountId);
    outState.putString(KEY_CURRENCY, currency);
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
          setUri(restoredUri);
          mFilename.setText(displayName);
        }
      }
    }
  }

  public static CsvImportParseFragment newInstance() {
    return new CsvImportParseFragment();
  }

  @Override
  public void onClick(View v) {
    DialogUtils.openBrowse(mUri, this);
  }

  @Override
  public boolean checkTypeParts(String[] typeParts, String extension) {
    return ImportFileResultHandler.checkTypePartsDefault(typeParts);
  }

  @Override
  public String getTypeName() {
    return "CSV";
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.csv_parse, menu);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    menu.findItem(R.id.PARSE_COMMAND).setEnabled(mUri != null);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.PARSE_COMMAND:
        QifDateFormat format = (QifDateFormat) mDateFormatSpinner.getSelectedItem();
        String encoding = (String) mEncodingSpinner.getSelectedItem();
        String delimiter = getResources().getStringArray(R.array.pref_csv_import_delimiter_values)
            [mDelimiterSpinner.getSelectedItemPosition()];
        MyApplication.getInstance().getSettings().edit()
            .putString(PREFKEY_IMPORT_CSV_DELIMITER, delimiter)
            .putString(PREFKEY_IMPORT_CSV_ENCODING, encoding)
            .putString(PREFKEY_IMPORT_CSV_DATE_FORMAT, format.name())
            .apply();
        ImportFileResultHandler.maybePersistUri(this);
        TaskExecutionFragment taskExecutionFragment =
            TaskExecutionFragment.newInstanceCSVParse(
                mUri, delimiter.charAt(0), encoding);
        getParentFragmentManager()
            .beginTransaction()
            .add(taskExecutionFragment, ASYNC_TAG)
            .add(ProgressDialogFragment.newInstance(
                getString(R.string.pref_import_title, "CSV"),
                null, ProgressDialog.STYLE_SPINNER, false), PROGRESS_TAG)
            .commit();
        break;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    if (getActivity() == null) {
      return null;
    }
    return new CursorLoader(
        getActivity(),
        TransactionProvider.ACCOUNTS_BASE_URI,
        new String[]{
            KEY_ROWID,
            KEY_LABEL,
            KEY_CURRENCY,
            KEY_TYPE},
        KEY_SEALED + " = 0 ", null, null);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    MatrixCursor extras = new MatrixCursor(new String[]{
        KEY_ROWID,
        KEY_LABEL,
        KEY_CURRENCY,
        KEY_TYPE
    });
    extras.addRow(new String[]{
        "0",
        getString(R.string.menu_create_account),
        Utils.getHomeCurrency().code(),
        AccountType.CASH.name()
    });
    mAccountsCursor = new MergeCursor(new Cursor[]{extras, data});
    mAccountsAdapter.swapCursor(mAccountsCursor);
    UiUtils.selectSpinnerItemByValue(mAccountSpinner, accountId);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    mAccountsCursor = null;
    mAccountsAdapter.swapCursor(null);
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position,
                             long id) {
    if (parent.getId() == R.id.Currency) {
      if (accountId == 0) {
        currency = ((Currency) parent.getSelectedItem()).code();
      }
      return;
    }
    if (parent.getId() == R.id.AccountType) {
      if (accountId == 0) {
        type = (AccountType) parent.getSelectedItem();
      }
      return;
    }
    //account selection
    if (mAccountsCursor != null) {
      accountId = id;
      mAccountsCursor.moveToPosition(position);

      String currency = (accountId == 0 && this.currency != null) ?
          this.currency :
          mAccountsCursor.getString(2);//2=KEY_CURRENCY
      AccountType type = (accountId == 0 && this.type != null) ?
          this.type :
          AccountType.valueOf(
              mAccountsCursor.getString(3));//3=KEY_TYPE
      mCurrencySpinner.setSelection(
          ((ArrayAdapter<Currency>) mCurrencySpinner.getAdapter())
              .getPosition(Currency.create(currency)));
      mTypeSpinner.setSelection(type.ordinal());
      mCurrencySpinner.setEnabled(position == 0);
      mTypeSpinner.setEnabled(position == 0);
    }
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
  }

  public long getAccountId() {
    return mAccountSpinner.getSelectedItemId();
  }

  public String getCurrency() {
    return ((Currency) mCurrencySpinner.getSelectedItem()).code();
  }

  public QifDateFormat getDateFormat() {
    return (QifDateFormat) mDateFormatSpinner.getSelectedItem();
  }

  public AccountType getAccountType() {
    return (AccountType) mTypeSpinner.getSelectedItem();
  }
}
