package org.totschnig.myexpenses.preference;

import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.totschnig.myexpenses.R;

public class FontSizeDialogFragmentCompat extends PreferenceDialogFragmentCompat {
  @Override
  protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
    final FontSizeDialogPreference preference = (FontSizeDialogPreference) getPreference();
    int selectedIndex = preference.getValue();
    final TypedArray a = getActivity().obtainStyledAttributes(null, android.support.v7.appcompat.R.styleable.AlertDialog,
        android.support.v7.appcompat.R.attr.alertDialogStyle, 0);
    //noinspection PrivateResource
    ListAdapter adapter = new ArrayAdapter<String>(
        getActivity(),
        a.getResourceId(android.support.v7.appcompat.R.styleable.AlertDialog_singleChoiceItemLayout, 0),
        preference.getEntries()) {
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        TextView row = (TextView) super.getView(position, convertView, parent);
        int size;
        switch (position) {
          case 1:
            size = R.dimen.textSizeMediumS1;
            break;
          case 2:
            size = R.dimen.textSizeMediumS2;
            break;
          case 3:
            size = R.dimen.textSizeMediumS3;
            break;
          default:
            size = R.dimen.textSizeMediumBase;
        }
        row.setTextSize(TypedValue.COMPLEX_UNIT_PX, getContext().getResources().getDimensionPixelSize(size));
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

  public static FontSizeDialogFragmentCompat newInstance(String key) {
    FontSizeDialogFragmentCompat fragment = new FontSizeDialogFragmentCompat();
    Bundle bundle = new Bundle(1);
    bundle.putString(ARG_KEY, key);
    fragment.setArguments(bundle);
    return fragment;
  }
}
