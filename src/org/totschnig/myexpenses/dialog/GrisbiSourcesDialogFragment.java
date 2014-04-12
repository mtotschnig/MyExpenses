package org.totschnig.myexpenses.dialog;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.GrisbiImport;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.View;

public class GrisbiSourcesDialogFragment extends ImportSourceDialogFragment implements
DialogInterface.OnClickListener {
  
  static final String PREFKEY_IMPORT_GRISBI_FILE_PATH = "import_grisbi_file_path";
  
  public static final GrisbiSourcesDialogFragment newInstance() {
    return new GrisbiSourcesDialogFragment();
  }
  @Override
  protected int getLayoutId() {
    return R.layout.grisbi_import_dialog;
  }
  @Override
  protected int getLayoutTitle() {
    return R.string.pref_import_from_grisbi_title;
  }

  @Override
  public void onClick(DialogInterface dialog, int id) {
    if (id == AlertDialog.BUTTON_POSITIVE) {
      String fileName = mFilename.getText().toString();
      MyApplication.getInstance().getSettings().edit()
      .putString(PREFKEY_IMPORT_GRISBI_FILE_PATH, fileName)
      .commit();
      ((GrisbiImport) getActivity()).onSourceSelected(
         fileName,
          mImportCategories.isChecked(),
          mImportParties.isChecked()
          );
    } else {
      super.onClick(dialog, id);
    }
  }
  @Override
  protected void setupDialogView(View view) {
    super.setupDialogView(view);
    mImportTransactions.setVisibility(View.GONE);
  }
  @Override
  public void onStart() {
    super.onStart();
    if (TextUtils.isEmpty(mFilename.getText().toString())) {
      mFilename.setText(MyApplication.getInstance().getSettings()
          .getString(PREFKEY_IMPORT_GRISBI_FILE_PATH, ""));
    }
  }
}
