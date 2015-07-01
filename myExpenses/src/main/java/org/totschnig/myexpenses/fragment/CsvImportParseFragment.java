package org.totschnig.myexpenses.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.dialog.DialogUtils;

/**
 * Created by privat on 30.06.15.
 */
public class CsvImportParseFragment extends Fragment implements View.OnClickListener, DialogUtils.UriTypePartChecker {
  static final String PREFKEY_IMPORT_CSV_DATE_FORMAT = "import_csv_date_format";
  static final String PREFKEY_IMPORT_CSV_ENCODING = "import_csv_encoding";
  private Uri mUri;
  private EditText mFilename;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.import_csv, container, false);
    DialogUtils.configureDateFormat(view, getActivity(), PREFKEY_IMPORT_CSV_DATE_FORMAT);
    DialogUtils.configureEncoding(view, getActivity(), PREFKEY_IMPORT_CSV_ENCODING);
    mFilename = DialogUtils.configureFilename(view);
    view.findViewById(R.id.btn_browse).setOnClickListener(this);
    return view;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == ProtectedFragmentActivity.IMPORT_FILENAME_REQUESTCODE) {
      if (resultCode == Activity.RESULT_OK && data != null) {
        mUri = DialogUtils.handleFilenameRequestResult(data, mFilename, "CSV", this);
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
}
