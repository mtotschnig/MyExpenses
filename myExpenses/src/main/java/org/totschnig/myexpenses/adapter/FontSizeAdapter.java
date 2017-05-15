package org.totschnig.myexpenses.adapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.preference.FontSizeDialogPreference;

public class FontSizeAdapter extends ArrayAdapter<String> {
  public FontSizeAdapter(Context context) {
    super(context, getItemLayoutResourceId(context), FontSizeDialogPreference.getEntries(context));
  }

  private static int getItemLayoutResourceId(Context context) {
    final TypedArray a = context.obtainStyledAttributes(null, android.support.v7.appcompat.R.styleable.AlertDialog,
        android.support.v7.appcompat.R.attr.alertDialogStyle, 0);
    //noinspection PrivateResource
    int resId = a.getResourceId(android.support.v7.appcompat.R.styleable.AlertDialog_singleChoiceItemLayout, 0);
    a.recycle();
    return resId;
  }

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
}
