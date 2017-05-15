package org.totschnig.myexpenses.preference;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceDialogFragmentCompat;

import org.totschnig.myexpenses.adapter.FontSizeAdapter;

import static android.content.DialogInterface.BUTTON_POSITIVE;

public class FontSizeDialogFragmentCompat extends PreferenceDialogFragmentCompat {
  @Override
  protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
    final FontSizeDialogPreference preference = (FontSizeDialogPreference) getPreference();
    int selectedIndex = preference.getValue();
    builder.setSingleChoiceItems(new FontSizeAdapter(getActivity()), selectedIndex, (dialog, which) -> {
      if (preference.callChangeListener(which)) {
        preference.setValue(which);
      }
      FontSizeDialogFragmentCompat.this.onClick(dialog, BUTTON_POSITIVE);
      dialog.dismiss();
    });
    builder.setPositiveButton(null, null);
  }

  @Override
  public void onDialogClosed(boolean b) {

  }

  public static FontSizeDialogFragmentCompat newInstance(String key) {
    FontSizeDialogFragmentCompat fragment = new FontSizeDialogFragmentCompat();
    Bundle bundle = new Bundle(1);
    bundle.putString(ARG_KEY, key);
    fragment.setArguments(bundle);
    return fragment;
  }
}
