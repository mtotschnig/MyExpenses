package org.totschnig.myexpenses.fragment;

import android.app.Dialog;
import android.os.Bundle;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.adapter.FontSizeAdapter;
import org.totschnig.myexpenses.preference.PrefKey;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class FontSizeDialogFragment extends DialogFragment {

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    int selectedIndex = PrefKey.UI_FONTSIZE.getInt(0);
    return new AlertDialog.Builder(getActivity())
        .setTitle(R.string.pref_ui_fontsize_title)
        .setSingleChoiceItems(new FontSizeAdapter(getActivity()), selectedIndex, (dialog, which) -> {
          PrefKey.UI_FONTSIZE.putInt(which);
          dismiss();
          getActivity().recreate();
        })
        .setNegativeButton(android.R.string.cancel, null)
        .create();
  }

  public static FontSizeDialogFragment newInstance() {
    return new FontSizeDialogFragment();
  }
}
