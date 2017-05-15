package org.totschnig.myexpenses.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.adapter.FontSizeAdapter;

public class FontSizeDialogFragment extends DialogFragment {

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new AlertDialog.Builder(getActivity())
        .setTitle(R.string.pref_ui_fontsize_title)
        .setSingleChoiceItems(new FontSizeAdapter(getActivity()), -1, (dialog, which) -> {
          dismiss();
        })
        .setNegativeButton(android.R.string.cancel, null)
        .create();
  }

  public static FontSizeDialogFragment newInstance(String key) {
    FontSizeDialogFragment fragment = new FontSizeDialogFragment();
    return fragment;
  }

}
