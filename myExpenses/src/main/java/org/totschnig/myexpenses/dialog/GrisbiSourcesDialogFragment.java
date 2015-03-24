package org.totschnig.myexpenses.dialog;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.GrisbiImport;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;

public class GrisbiSourcesDialogFragment extends TextSourceDialogFragment implements
DialogInterface.OnClickListener {
  
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
  String getTypeName() {
    return "Grisbi XML";
  }
  @Override
  String getPrefKey() {
    return "import_grisbi_file_uri";
  }

  @Override
  public void onClick(DialogInterface dialog, int id) {
    if (getActivity()==null) {
      return;
    }
    if (id == AlertDialog.BUTTON_POSITIVE) {
      SharedPreferencesCompat.apply(
        MyApplication.getInstance().getSettings().edit()
        .putString(getPrefKey(), mUri.toString()));
      ((GrisbiImport) getActivity()).onSourceSelected(
          mUri,
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
}
