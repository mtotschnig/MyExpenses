package org.totschnig.myexpenses.adapter;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_SUB;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Account.Grouping;
import org.totschnig.myexpenses.model.Account.Type;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter;
import org.totschnig.myexpenses.util.Utils;

import android.content.Context;
import android.database.Cursor;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

public class TransactionAdapter extends SimpleCursorAdapter {
  private int dateEms;
  private Account mAccount;
  private Grouping mGroupingOverride;
  DateFormat localizedTimeFormat,itemDateFormat;
  private int colorExpense;
  private int colorIncome;

  public TransactionAdapter(Account account, Grouping grouping, Context context, int layout, Cursor c, String[] from,
      int[] to, int flags) {
    super(context, layout, c, from, to, flags);
    colorIncome = ((ProtectedFragmentActivity) context).getColorIncome();
    colorExpense = ((ProtectedFragmentActivity) context).getColorExpense();
    mAccount = account;
    mGroupingOverride = grouping;
    dateEms = android.text.format.DateFormat.is24HourFormat(context) ? 3 : 4;
    localizedTimeFormat = android.text.format.DateFormat.getTimeFormat(context);
    refreshDateFormat();
  }
  public TransactionAdapter(Account account, Context context, int layout, Cursor c, String[] from,
      int[] to, int flags) {
    this(account, null, context, layout, c, from, to, flags);
  }
  @Override
  public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v= super.newView(context, cursor, parent);
    ViewHolder holder = new ViewHolder();
    View colorContainer = v.findViewById(R.id.colorContainer);
    View colorAccount = v.findViewById(R.id.colorAccount);
    holder.colorContainer = colorContainer;
    holder.colorAccount = colorAccount;
    holder.amount = (TextView) v.findViewById(R.id.amount);
    holder.category = (TextView) v.findViewById(R.id.category);
    holder.color1 = v.findViewById(R.id.color1);
    if (mAccount.type.equals(Type.CASH)) {
      colorContainer.setVisibility(View.GONE);
    }
    if (mAccount.getId() < 0) {
      colorAccount.setLayoutParams(
          new LayoutParams(4, LayoutParams.FILL_PARENT));
    }
    TextView tv = (TextView) v.findViewById(R.id.date);
    tv.setEms(dateEms);
    v.setTag(holder);
    return v;
}
  /* (non-Javadoc)
   * calls {@link #convText for formatting the values retrieved from the cursor}
   * @see android.widget.SimpleCursorAdapter#setViewText(android.widget.TextView, java.lang.String)
   */
  @Override
  public void setViewText(TextView v, String text) {
    switch (v.getId()) {
    case R.id.date:
      text = Utils.convDateTime(text,itemDateFormat);
      break;
    case R.id.amount:
      text = Utils.convAmount(text,mAccount.currency);
    }
    super.setViewText(v, text);
  }
  /**
   * @param c
   * @return extracts the information that should
   * be displayed about the mapped category, can be overridden by subclass
   * should not be used for handle transfers
   */
  protected CharSequence getCatText(CharSequence catText,String label_sub) {
    if (label_sub != null && label_sub.length() > 0) {
      catText = catText + TransactionList.CATEGORY_SEPARATOR + label_sub;
    }
    return catText;
  }
  /* (non-Javadoc)
   * manipulates the view for amount (setting expenses to red) and
   * category (indicate transfer direction with => or <=
   * @see android.widget.CursorAdapter#getView(int, android.view.View, android.view.ViewGroup)
   */
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    convertView=super.getView(position, convertView, parent);
    ViewHolder viewHolder = (ViewHolder) convertView.getTag();
    TextView tv1 = viewHolder.amount;
    Cursor c = getCursor();
    c.moveToPosition(position);
    if (mAccount.getId() <0) {
      int color = c.getInt(c.getColumnIndex("color"));
      viewHolder.colorAccount.setBackgroundColor(color);
    }
    long amount = c.getLong(c.getColumnIndex(KEY_AMOUNT));
    tv1.setTextColor(amount<0?colorExpense:colorIncome);
    TextView tv2 = viewHolder.category;
    CharSequence catText = tv2.getText();
    if (DbUtils.getLongOrNull(c,c.getColumnIndex(KEY_TRANSFER_PEER)) != null) {
      catText = ((amount < 0) ? "=> " : "<= ") + catText;
    } else {
      Long catId = DbUtils.getLongOrNull(c,KEY_CATID);
      if (SPLIT_CATID.equals(catId))
        catText = MyApplication.getInstance().getString(R.string.split_transaction);
      else if (catId == null) {
        catText = MyApplication.getInstance().getString(R.string.no_category_assigned);
      } else {
        catText = getCatText(catText,c.getString(c.getColumnIndex(KEY_LABEL_SUB)));
      }
    }
    String referenceNumber= c.getString(c.getColumnIndex(KEY_REFERENCE_NUMBER));
    if (referenceNumber != null && referenceNumber.length() > 0)
      catText = "(" + referenceNumber + ") " + catText;
    SpannableStringBuilder ssb;
    String comment = c.getString(c.getColumnIndex(KEY_COMMENT));
    if (comment != null && comment.length() > 0) {
      ssb = new SpannableStringBuilder(comment);
      ssb.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), 0, comment.length(), 0);
      catText = TextUtils.concat(catText,TransactionList.COMMENT_SEPARATOR,ssb);
    }
    String payee = c.getString(c.getColumnIndex(KEY_PAYEE_NAME));
    if (payee != null && payee.length() > 0) {
      ssb = new SpannableStringBuilder(payee);
      ssb.setSpan(new UnderlineSpan(), 0, payee.length(), 0);
      catText = TextUtils.concat(catText,TransactionList.COMMENT_SEPARATOR,ssb);
    }
    tv2.setText(catText);
    
    if (!mAccount.type.equals(Type.CASH)) {
      CrStatus status;
      try {
        status = CrStatus.valueOf(c.getString(c.getColumnIndex(KEY_CR_STATUS)));
      } catch (IllegalArgumentException ex) {
        status = CrStatus.UNRECONCILED;
      }
      viewHolder.color1.setBackgroundColor(status.color);
      viewHolder.colorContainer.setTag(status == CrStatus.RECONCILED ? -1 : getItemId(position));
    }
    return convertView;
  }
  public void refreshDateFormat() {
    switch (mGroupingOverride!=null ? mGroupingOverride : mAccount.grouping) {
    case DAY:
      itemDateFormat = localizedTimeFormat;
      break;
    case MONTH:
      itemDateFormat = new SimpleDateFormat("dd");
      break;
    case WEEK:
      itemDateFormat = new SimpleDateFormat("EEE");
      break;
    case YEAR:
    case NONE:
      itemDateFormat = Utils.localizedYearlessDateFormat();
    }
  }
  class ViewHolder {
    TextView amount;
    View colorAccount;
    TextView category;
    View color1;
    View colorContainer;
  }
}