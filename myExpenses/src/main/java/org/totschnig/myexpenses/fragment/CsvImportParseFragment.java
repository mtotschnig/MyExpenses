package org.totschnig.myexpenses.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.export.qif.QifDateFormat;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;
import org.totschnig.myexpenses.task.TaskExecutionFragment;


/**
 * Created by privat on 30.06.15.
 */
public class CsvImportParseFragment extends Fragment implements View.OnClickListener, DialogUtils.UriTypePartChecker {
  static final String PREFKEY_IMPORT_CSV_DATE_FORMAT = "import_csv_date_format";
  static final String PREFKEY_IMPORT_CSV_ENCODING = "import_csv_encoding";
  static final String PREFKEY_IMPORT_CSV_DELIMITER = "import_csv_delimiter";
  private Uri mUri;
  public void setmUri(Uri mUri) {
    this.mUri = mUri;
    getActivity().supportInvalidateOptionsMenu();
  }
  private EditText mFilename;
  private Spinner mDateFormatSpinner, mEncodingSpinner, mDelimiterSpinner;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.import_csv_parse, container, false);
    mDateFormatSpinner = DialogUtils.configureDateFormat(view, getActivity(), PREFKEY_IMPORT_CSV_DATE_FORMAT);
    mEncodingSpinner = DialogUtils.configureEncoding(view, getActivity(), PREFKEY_IMPORT_CSV_ENCODING);
    mDelimiterSpinner = DialogUtils.configureDelimiter(view, getActivity(), PREFKEY_IMPORT_CSV_DELIMITER);
    mFilename = DialogUtils.configureFilename(view);
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
    inflater.inflate(R.menu.cvs_parse, menu);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    menu.findItem(R.id.PARSE_COMMAND).setEnabled(mUri!=null);
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
                "ASYNC_TASK")
            .add(ProgressDialogFragment.newInstance(
                    getString(R.string.pref_import_title, "CSV"),
                    null, ProgressDialog.STYLE_SPINNER, false),
                "PROGRESS")
            .commit();
        break;
    }
    return super.onOptionsItemSelected(item);
  }

}
