package org.totschnig.myexpenses.adapter;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_SUB;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER;

import java.util.Currency;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter;
import org.totschnig.myexpenses.util.Utils;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.text.Html;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public final class SplitPartAdapter extends SimpleCursorAdapter {
  private final String commentSeparator = " / ";
  private final String categorySeparator = " : ";
  int colorExpense;
  int colorIncome;
  Currency mCurrency;
  boolean insideFragment;

  public SplitPartAdapter(Context context, int layout, Cursor c,
      String[] from, int[] to, int flags, Currency currency) {
    super(context, layout, c, from, to, flags);
    if (context instanceof MyExpenses) {
      insideFragment = true;
    }
    Resources.Theme theme = context.getTheme();
    TypedValue color = new TypedValue();
    theme.resolveAttribute(R.attr.colorExpense, color, true);
    colorExpense = color.data;
    theme.resolveAttribute(R.attr.colorIncome,color, true);
    colorIncome = color.data;
    mCurrency = currency;
  }

  /* (non-Javadoc)
   * calls {@link #convText for formatting the values retrieved from the cursor}
   * @see android.widget.SimpleCursorAdapter#setViewText(android.widget.TextView, java.lang.String)
   */
  @Override
  public void setViewText(TextView v, String text) {
    switch (v.getId()) {
    case R.id.amount:
      text = Utils.convAmount(text,mCurrency);
    }
    super.setViewText(v, text);
  }

  /* (non-Javadoc)
   * manipulates the view for amount (setting expenses to red) and
   * category (indicate transfer direction with => or <=
   * @see android.widget.CursorAdapter#getView(int, android.view.View, android.view.ViewGroup)
   */
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View row=super.getView(position, convertView, parent);
    TextView tv1 = (TextView)row.findViewById(R.id.amount);
    Cursor c = getCursor();
    c.moveToPosition(position);
    int col = c.getColumnIndex(KEY_AMOUNT);
    long amount = c.getLong(col);
    if (amount < 0) {
      tv1.setTextColor(colorExpense);
      // Set the background color of the text.
    }
    else {
      tv1.setTextColor(colorIncome);
    }
    TextView tv2 = (TextView)row.findViewById(R.id.category);
    if (insideFragment && Build.VERSION.SDK_INT < 11) {
      tv2.setTextColor(Color.WHITE);
    }
    String catText = tv2.getText().toString();
    if (DbUtils.getLongOrNull(c,KEY_TRANSFER_PEER) != null) {
      catText = ((amount < 0) ? "=&gt; " : "&lt;= ") + catText;
    } else {
      Long catId = DbUtils.getLongOrNull(c,KEY_CATID);
      if (catId == null) {
        catText = MyApplication.getInstance().getString(R.string.no_category_assigned);
      }
      else {
        col = c.getColumnIndex(KEY_LABEL_SUB);
        String label_sub = c.getString(col);
        if (label_sub != null && label_sub.length() > 0) {
          catText += categorySeparator + label_sub;
        }
      }
    }
    col = c.getColumnIndex(KEY_COMMENT);
    String comment = c.getString(col);
    if (comment != null && comment.length() > 0) {
      catText += (catText.equals("") ? "" : commentSeparator) + "<i>" + comment + "</i>";
    }
    tv2.setText(Html.fromHtml(catText));
    return row;
  }
}