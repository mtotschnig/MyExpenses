package org.totschnig.myexpenses.dialog;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.GrisbiImport;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;

public class GrisbiSourcesDialogFragment extends ImportSourceDialogFragment implements
DialogInterface.OnClickListener {
  
  static final String PREFKEY_IMPORT_GRISBI_FILE_URI = "import_grisbi_file_uri";
  
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
  public void onClick(DialogInterface dialog, int id) {
    if (id == AlertDialog.BUTTON_POSITIVE) {
      MyApplication.getInstance().getSettings().edit()
      .putString(PREFKEY_IMPORT_GRISBI_FILE_URI, mUri.toString())
      .commit();
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
  @Override
  public void onStart() {
    if (mUri==null) {
      String storedUri = MyApplication.getInstance().getSettings()
          .getString(PREFKEY_IMPORT_GRISBI_FILE_URI, "");
      if (!storedUri.equals("")) {
        mUri = Uri.parse(storedUri);
        mFilename.setText(getDisplayName(mUri));
      }
    }
    super.onStart();
  }
}
