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
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.activity.ProtectionDelegate;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.export.qif.QifDateFormat;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter;
import org.totschnig.myexpenses.util.Utils;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;


/**
 * Created by privat on 30.06.15.
 */
public class CsvImportParseFragment extends Fragment implements View.OnClickListener,
    DialogUtils.UriTypePartChecker, LoaderManager.LoaderCallbacks<Cursor>,
    AdapterView.OnItemSelectedListener {
  static final String PREFKEY_IMPORT_CSV_DATE_FORMAT = "import_csv_date_format";
  static final String PREFKEY_IMPORT_CSV_ENCODING = "import_csv_encoding";
  static final String PREFKEY_IMPORT_CSV_DELIMITER = "import_csv_delimiter";
  private Uri mUri;
  public void setmUri(Uri mUri) {
    this.mUri = mUri;
    getActivity().supportInvalidateOptionsMenu();
  }
  private EditText mFilename;
  private Spinner mDateFormatSpinner, mEncodingSpinner, mDelimiterSpinner, mAccountSpinner,
      mCurrencySpinner, mTypeSpinner;
  private MergeCursor mAccountsCursor;
  private SimpleCursorAdapter mAccountsAdapter;
  private long accountId = 0;
  private Account.CurrencyEnum currency = null;
  private Account.Type type = null;


  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    if (savedInstanceState!=null) {
        accountId = savedInstanceState.getLong(KEY_ACCOUNTID);
        currency = (Account.CurrencyEnum) savedInstanceState.getSerializable(KEY_CURRENCY);
        type = (Account.Type) savedInstanceState.getSerializable(KEY_TYPE);
    }

    View view = inflater.inflate(R.layout.import_csv_parse, container, false);
    mDateFormatSpinner = DialogUtils.configureDateFormat(view, getActivity(), PREFKEY_IMPORT_CSV_DATE_FORMAT);
    mEncodingSpinner = DialogUtils.configureEncoding(view, getActivity(), PREFKEY_IMPORT_CSV_ENCODING);
    mDelimiterSpinner = DialogUtils.configureDelimiter(view, getActivity(), PREFKEY_IMPORT_CSV_DELIMITER);
    mFilename = DialogUtils.configureFilename(view);
    mAccountSpinner = (Spinner) view.findViewById(R.id.Account);
    Context wrappedCtx = view.getContext();
    mAccountsAdapter = new SimpleCursorAdapter(wrappedCtx , android.R.layout.simple_spinner_item, null,
        new String[] {KEY_LABEL}, new int[] {android.R.id.text1}, 0);
    mAccountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mAccountSpinner.setAdapter(mAccountsAdapter);
    mAccountSpinner.setOnItemSelectedListener(this);
    mCurrencySpinner = DialogUtils.configureCurrencySpinner(view,wrappedCtx,this);
    mTypeSpinner = DialogUtils.configureTypeSpinner(view,wrappedCtx);
    mTypeSpinner.setOnItemSelectedListener(this);
    getLoaderManager().initLoader(0, null, this);
    view.findViewById(R.id.btn_browse).setOnClickListener(this);
    return view;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == ProtectedFragmentActivity.IMPORT_FILENAME_REQUESTCODE) {
      if (resultCode == Activity.RESULT_OK && data != null) {
        setmUri(DialogUtils.handleFilenameRequestResult(data, mFilename, "CSV", this));
      }
    }
  }
  String getPrefKey() {
    return "import_csv_file_uri";
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
          setmUri(restoredUri);
          mFilename.setText(displayName);
        }
      }
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (mUri != null) {
      outState.putString(getPrefKey(), mUri.toString());
    }
    outState.putLong(KEY_ACCOUNTID, accountId);
    outState.putSerializable(KEY_CURRENCY, currency);
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
          setmUri(restoredUri);
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
  public boolean checkTypeParts(String[] typeParts) {
   return DialogUtils.checkTypePartsDefault(typeParts);
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
        SharedPreferencesCompat.apply(
            MyApplication.getInstance().getSettings().edit()
                .putString(getPrefKey(), mUri.toString())
                .putString(PREFKEY_IMPORT_CSV_DELIMITER, delimiter)
                .putString(PREFKEY_IMPORT_CSV_ENCODING, encoding)
                .putString(PREFKEY_IMPORT_CSV_DATE_FORMAT, format.name()));
        TaskExecutionFragment taskExecutionFragment =
            TaskExecutionFragment.newInstanceCSVParse(
                mUri, delimiter.charAt(0), encoding);
        getFragmentManager()
            .beginTransaction()
            .add(taskExecutionFragment,
                ProtectionDelegate.ASYNC_TAG)
            .add(ProgressDialogFragment.newInstance(
                    getString(R.string.pref_import_title, "CSV"),
                    null, ProgressDialog.STYLE_SPINNER, false),
                ProtectionDelegate.PROGRESS_TAG)
            .commit();
        break;
    }
    return super.onOptionsItemSelected(item);
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
            KEY_CURRENCY,
            KEY_TYPE},
        null,null, null);
    return cursorLoader;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    MatrixCursor extras = new MatrixCursor(new String[] {
        KEY_ROWID,
        KEY_LABEL,
        KEY_CURRENCY,
        KEY_TYPE
    });
    extras.addRow(new String[] {
        "0",
        getString(R.string.menu_create_account),
        Account.getLocaleCurrency().getCurrencyCode(),
        Account.Type.CASH.name()

    });
    mAccountsCursor = new MergeCursor(new Cursor[] {extras,data});
    mAccountsAdapter.swapCursor(mAccountsCursor);
    Utils.selectSpinnerItemByValue(mAccountSpinner, accountId);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    mAccountsCursor = null;
    mAccountsAdapter.swapCursor(null);
  }
  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position,
                             long id) {
    if (parent.getId()==R.id.Currency) {
      if (accountId==0) {
        currency = (Account.CurrencyEnum) parent.getSelectedItem();
      }
      return;
    }
    if (parent.getId()==R.id.AccountType) {
      if (accountId==0) {
        type = (Account.Type) parent.getSelectedItem();
      }
      return;
    }
    //account selection
    if (mAccountsCursor != null) {
      accountId = id;
      mAccountsCursor.moveToPosition(position);

      Account.CurrencyEnum currency = (accountId==0 && this.currency !=null) ?
          this.currency :
          Account.CurrencyEnum
              .valueOf(
                  mAccountsCursor.getString(2));//2=KEY_CURRENCY
      Account.Type type = (accountId==0 && this.type !=null) ?
          this.type :
          Account.Type.valueOf(
                  mAccountsCursor.getString(3));//3=KEY_TYPE
      mCurrencySpinner.setSelection(
          ((ArrayAdapter<Account.CurrencyEnum>) mCurrencySpinner.getAdapter())
              .getPosition(currency));
      mTypeSpinner.setSelection(type.ordinal());
      mCurrencySpinner.setEnabled(position == 0);
      mTypeSpinner.setEnabled(position==0);
    }
  }
  @Override
  public void onNothingSelected(AdapterView<?> parent) {
  }

  public long getAccountId() {
    return mAccountSpinner.getSelectedItemId();
  }
  public String getCurrency() {
    return ((Account.CurrencyEnum) mCurrencySpinner.getSelectedItem()).name();
  }

  public QifDateFormat getDateFormat() {
    return (QifDateFormat) mDateFormatSpinner.getSelectedItem();
  }

  public Account.Type getAccountType() {
    return (Account.Type) mTypeSpinner.getSelectedItem();
  }
}
