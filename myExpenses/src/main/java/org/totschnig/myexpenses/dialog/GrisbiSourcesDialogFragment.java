package org.totschnig.myexpenses.dialog;

import android.content.DialogInterface;
import android.view.View;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.GrisbiImport;

import androidx.appcompat.app.AlertDialog;

public class GrisbiSourcesDialogFragment extends TextSourceDialogFragment implements
DialogInterface.OnClickListener {
  
  public static GrisbiSourcesDialogFragment newInstance() {
    return new GrisbiSourcesDialogFragment();
  }
  @Override
  protected int getLayoutId() {
    return R.layout.grisbi_import_dialog;
  }
  @Override
  protected String getLayoutTitle() {
    return getString(R.string.pref_import_from_grisbi_title);
  }

  @Override
  public String getTypeName() {
    return "Grisbi XML";
  }
  @Override
  public String getPrefKey() {
    return "import_grisbi_file_uri";
  }

  @Override
  public void onClick(DialogInterface dialog, int id) {
    if (getActivity()==null) {
      return;
    }
    if (id == AlertDialog.BUTTON_POSITIVE) {
      maybePersistUri();
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
