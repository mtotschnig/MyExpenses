package org.totschnig.myexpenses.preference;

import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.totschnig.myexpenses.R;

/**
 * Created by privat on 07.11.15.
 */
public class FontSizeDialogFragmentCompat extends PreferenceDialogFragmentCompat {
  @Override
  protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
    final FontSizePreference preference = (FontSizePreference) getPreference();
    int selectedIndex = preference.getValue();
    String standard = getString(R.string.pref_ui_language_default);
    final TypedArray a = getActivity().obtainStyledAttributes(null, android.support.v7.appcompat.R.styleable.AlertDialog,
        android.support.v7.appcompat.R.attr.alertDialogStyle, 0);
    ListAdapter adapter = new ArrayAdapter<String>(
        getActivity(),
        a.getResourceId(android.support.v7.appcompat.R.styleable.AlertDialog_singleChoiceItemLayout, 0),
        new String[]{standard, standard + " 2sp", standard + " 4sp", standard + " + 6sp"}) {
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        TextView row = (TextView) super.getView(position, convertView, parent);
        int style;
        switch (position) {
          case 1:
            style = R.style.MyTextAppearanceMedium_s1;
            break;
          case 2:
            style = R.style.MyTextAppearanceMedium_s2;
            break;
          case 3:
            style = R.style.MyTextAppearanceMedium_s3;
            break;
          default:
            style = R.style.MyTextAppearanceMedium;
        }
        row.setTextAppearance(getActivity(), style);
        return row;
      }
    };
    a.recycle();
    builder.setSingleChoiceItems(adapter, selectedIndex, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        if(preference.callChangeListener(which)) {
          preference.setValue(which);
        }
        FontSizeDialogFragmentCompat.this.onClick(dialog, -1);
        dialog.dismiss();
      }
    });
    builder.setPositiveButton((CharSequence) null, (DialogInterface.OnClickListener) null);
  }

  @Override
  public void onDialogClosed(boolean b) {

  }

  public static FontSizeDialogFragmentCompat newInstance(Preference preference) {
    FontSizeDialogFragmentCompat fragment = new FontSizeDialogFragmentCompat();
    Bundle bundle = new Bundle(1);
    bundle.putString("key", preference.getKey());
    fragment.setArguments(bundle);
    return fragment;
  }
}
