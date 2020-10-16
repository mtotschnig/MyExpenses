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
    final TypedArray a = context.obtainStyledAttributes(null, androidx.appcompat.R.styleable.AlertDialog,
        androidx.appcompat.R.attr.alertDialogStyle, 0);
    //noinspection PrivateResource
    int resId = a.getResourceId(androidx.appcompat.R.styleable.AlertDialog_singleChoiceItemLayout, 0);
    a.recycle();
    return resId;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    TextView row = (TextView) super.getView(position, convertView, parent);
    updateTextView(row, position, getContext());
    return row;
  }

  public static void updateTextView(TextView textView, int value, Context context) {
    float factor = 1 + value / 10F;
    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimensionPixelSize(R.dimen.textSizeSmall)* factor);
  }
}
