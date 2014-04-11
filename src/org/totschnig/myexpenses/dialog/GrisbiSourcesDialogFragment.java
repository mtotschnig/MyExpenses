package org.totschnig.myexpenses.dialog;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.GrisbiImport;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class GrisbiSourcesDialogFragment extends ImportSourceDialogFragment implements
DialogInterface.OnClickListener {
  
  public static final GrisbiSourcesDialogFragment newInstance() {
    return new GrisbiSourcesDialogFragment();
  }
  protected int getLayoutId() {
    return R.layout.grisbi_import_dialog;
  }
  protected int getLayoutTitle() {
    return R.string.pref_import_from_grisbi_title;
  }

  @Override
  public void onClick(DialogInterface dialog, int id) {
    if (id == AlertDialog.BUTTON_POSITIVE) {
      ((GrisbiImport) getActivity()).onSourceSelected(
          mFilename.getText().toString()
          );
    } else {
      super.onClick(dialog, id);
    }
  }
}
