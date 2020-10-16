package org.totschnig.myexpenses.preference;

import android.os.Bundle;

import org.totschnig.myexpenses.adapter.FontSizeAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceDialogFragmentCompat;

import static android.content.DialogInterface.BUTTON_POSITIVE;

public class FontSizeDialogFragmentCompat extends PreferenceDialogFragmentCompat {
  int mClickedDialogEntryIndex;
  @Override
  protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
    final FontSizeDialogPreference preference = (FontSizeDialogPreference) getPreference();
    int selectedIndex = preference.getValue();
    builder.setSingleChoiceItems(new FontSizeAdapter(getActivity()), selectedIndex, (dialog, which) -> {
      mClickedDialogEntryIndex = which;
      FontSizeDialogFragmentCompat.this.onClick(dialog, BUTTON_POSITIVE);
      dialog.dismiss();
    });
    builder.setPositiveButton(null, null);
  }

  @Override
  public void onDialogClosed(boolean positiveResult) {
    if (positiveResult && mClickedDialogEntryIndex >= 0) {
      final FontSizeDialogPreference preference = (FontSizeDialogPreference) getPreference();
      if (preference.callChangeListener(mClickedDialogEntryIndex)) {
        preference.setValue(mClickedDialogEntryIndex);
      }
    }
  }

  public static FontSizeDialogFragmentCompat newInstance(String key) {
    FontSizeDialogFragmentCompat fragment = new FontSizeDialogFragmentCompat();
    Bundle bundle = new Bundle(1);
    bundle.putString(ARG_KEY, key);
    fragment.setArguments(bundle);
    return fragment;
  }
}
