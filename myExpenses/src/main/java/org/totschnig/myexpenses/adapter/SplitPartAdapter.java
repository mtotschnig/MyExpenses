package org.totschnig.myexpenses.adapter;

import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.util.CurrencyFormatter;

import androidx.core.text.HtmlCompat;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_SUB;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT;

public final class SplitPartAdapter extends SimpleCursorAdapter {
  int colorExpense;
  int colorIncome;

  public void setCurrency(CurrencyUnit currency) {
    this.currency = currency;
  }

  CurrencyUnit currency;
  boolean insideFragment;

  private final CurrencyFormatter currencyFormatter;

  public SplitPartAdapter(ProtectedFragmentActivity context, int layout, Cursor c,
                          String[] from, int[] to, int flags, CurrencyUnit currency,
                          CurrencyFormatter currencyFormatter) {
    super(context, layout, c, from, to, flags);
    if (context instanceof MyExpenses) {
      insideFragment = true;
    }
    colorExpense = context.getResources().getColor(R.color.colorExpense);
    colorIncome = context.getResources().getColor(R.color.colorIncome);
    this.currency = currency;
    this.currencyFormatter = currencyFormatter;
  }

  /* (non-Javadoc)
   * calls {@link #convText for formatting the values retrieved from the cursor}
   * @see android.widget.SimpleCursorAdapter#setViewText(android.widget.TextView, java.lang.String)
   */
  @Override
  public void setViewText(TextView v, String text) {
    if (v.getId() == R.id.amount) {
      text = currencyFormatter.convAmount(text, currency);
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
    View row = super.getView(position, convertView, parent);
    TextView tv1 = (TextView) row.findViewById(R.id.amount);
    Cursor c = getCursor();
    c.moveToPosition(position);
    int col = c.getColumnIndex(KEY_AMOUNT);
    long amount = c.getLong(col);
    tv1.setTextColor(amount < 0 ? colorExpense : colorIncome);
    TextView tv2 = (TextView) row.findViewById(R.id.category);
    //should not be needed, even harmful //TODO check
    /*if (insideFragment && Build.VERSION.SDK_INT < 11) {
      tv2.setTextColor(Color.WHITE);
    }*/
    String catText = tv2.getText().toString();
    if (DbUtils.getLongOrNull(c, KEY_TRANSFER_ACCOUNT) != null) {
      catText = Transfer.getIndicatorPrefixForLabel(amount) + catText;
    } else {
      Long catId = DbUtils.getLongOrNull(c, KEY_CATID);
      if (catId == null) {
        catText = Category.NO_CATEGORY_ASSIGNED_LABEL;
      } else {
        col = c.getColumnIndex(KEY_LABEL_SUB);
        String label_sub = c.getString(col);
        if (label_sub != null && label_sub.length() > 0) {
          String categorySeparator = " : ";
          catText += categorySeparator + label_sub;
        }
      }
    }
    col = c.getColumnIndex(KEY_COMMENT);
    String comment = c.getString(col);
    if (comment != null && comment.length() > 0) {
      String commentSeparator = " / ";
      catText += (catText.equals("") ? "" : commentSeparator) + "<i>" + comment + "</i>";
    }
    tv2.setText(HtmlCompat.fromHtml(catText, HtmlCompat.FROM_HTML_MODE_LEGACY));
    return row;
  }
}