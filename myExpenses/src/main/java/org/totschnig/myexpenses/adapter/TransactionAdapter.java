package org.totschnig.myexpenses.adapter;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_SUB;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_HELPER;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageCategories;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter;
import org.totschnig.myexpenses.util.Utils;

import android.content.Context;
import android.database.Cursor;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
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
  boolean insideFragment;
  protected int monthStart =
      Integer.parseInt(PrefKey.GROUP_MONTH_STARTS.getString("1"));

  public TransactionAdapter(Account account, Grouping grouping, Context context, int layout, Cursor c, String[] from,
      int[] to, int flags) {
    super(context, layout, c, from, to, flags);
    if (context instanceof ManageCategories) {
      insideFragment = true;
    }
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
    holder.voidMarker = v.findViewById(R.id.voidMarker);
    TextView tv = (TextView) v.findViewById(R.id.date);
    holder.date = tv;
    if (mAccount.getId() < 0) {
      colorAccount.setLayoutParams(
          new LayoutParams(4, LayoutParams.FILL_PARENT));
    }
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
   * @param catText
   * @param label_sub
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
      else {
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
      catText = catText.length()>0 ?
          TextUtils.concat(catText,TransactionList.COMMENT_SEPARATOR,ssb):
          ssb;
    }
    String payee = c.getString(c.getColumnIndex(KEY_PAYEE_NAME));
    if (payee != null && payee.length() > 0) {
      ssb = new SpannableStringBuilder(payee);
      ssb.setSpan(new UnderlineSpan(), 0, payee.length(), 0);
      catText = catText.length()>0 ?
          TextUtils.concat(catText,TransactionList.COMMENT_SEPARATOR,ssb):
          ssb;
    }
    if (insideFragment) {
      if (catText.length()==0) {
        catText = "―――";
        tv2.setGravity(Gravity.CENTER);
      } else {
        tv2.setGravity(Gravity.START);
      }
    }
    tv2.setText(catText);

    CrStatus status;
    try {
      status = CrStatus.valueOf(c.getString(c.getColumnIndex(KEY_CR_STATUS)));
    } catch (IllegalArgumentException ex) {
      status = CrStatus.UNRECONCILED;
    }

    if (!mAccount.type.equals(AccountType.CASH) && !status.equals(CrStatus.VOID)) {
      viewHolder.color1.setBackgroundColor(status.color);
      viewHolder.colorContainer.setTag(status == CrStatus.RECONCILED ? -1 : getItemId(position));
      viewHolder.colorContainer.setVisibility(View.VISIBLE);
    } else {
      viewHolder.colorContainer.setVisibility(View.GONE);
    }
    viewHolder.voidMarker.setVisibility(status.equals(CrStatus.VOID) ? View.VISIBLE : View.GONE);
    return convertView;
  }
  public void refreshDateFormat() {
    switch (mGroupingOverride!=null ? mGroupingOverride : mAccount.grouping) {
    case DAY:
      itemDateFormat = localizedTimeFormat;
      break;
    case MONTH:
      //noinspection SimpleDateFormat
      itemDateFormat = monthStart == 1 ?
          new SimpleDateFormat("dd") : Utils.localizedYearlessDateFormat();
      break;
    case WEEK:
      //noinspection SimpleDateFormat
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
    TextView date;
    View voidMarker;
  }
}