package org.totschnig.myexpenses.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Typeface;
import android.support.v4.widget.ResourceCursorAdapter;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageCategories;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Currency;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_SAME_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_MAIN;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_SUB;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_HELPER;

public class TransactionAdapter extends ResourceCursorAdapter {
  private int dateEms;
  private Account mAccount;
  private Grouping mGroupingOverride;
  DateFormat localizedTimeFormat, itemDateFormat;
  private int colorExpense, colorIncome;
  ColorStateList textColorSecondary;
  boolean insideFragment;
  protected int monthStart =
      Integer.parseInt(PrefKey.GROUP_MONTH_STARTS.getString("1"));
  private CurrencyFormatter currencyFormatter;
  private boolean indexesCalculated = false;
  private int columnIndexDate;
  private int currencyIndexCurrency;
  private int columnIndexSameCurrency;
  private int columnIndexColor;
  private int columnIndexLabelMain;
  private int columnIndexAccountLabel;
  private int columnIndexStatus;
  private int columnIndexLabelSub;
  private int columnIndexReferenceNumber;
  private int columnIndexComment;
  private int columnIndexPayee;
  private int columnIndexCrStatus;
  private int columnIndexRowId;
  private int columnIndexAmount;
  private int columnIndexTransferPeer;

  protected TransactionAdapter(Account account, Grouping grouping, Context context, int layout,
                               Cursor c, int flags,
                               CurrencyFormatter currencyFormatter) {
    super(context, layout, c, flags);
    if (context instanceof ManageCategories) {
      insideFragment = true;
    }
    colorIncome = ((ProtectedFragmentActivity) context).getColorIncome();
    colorExpense = ((ProtectedFragmentActivity) context).getColorExpense();
    textColorSecondary = ((ProtectedFragmentActivity) context).getTextColorSecondary();
    mAccount = account;
    mGroupingOverride = grouping;
    dateEms = android.text.format.DateFormat.is24HourFormat(context) ? 3 : 4;
    localizedTimeFormat = android.text.format.DateFormat.getTimeFormat(context);
    this.currencyFormatter = currencyFormatter;
    refreshDateFormat();
  }

  public TransactionAdapter(Account account, Context context, int layout, Cursor c, int flags,
                            CurrencyFormatter currencyFormatter) {
    this(account, null, context, layout, c, flags, currencyFormatter);
  }

  @Override
  public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v = super.newView(context, cursor, parent);
    ViewHolder holder = new ViewHolder();
    View colorContainer = v.findViewById(R.id.colorContainer);
    View colorAccount = v.findViewById(R.id.colorAccount);
    holder.colorContainer = colorContainer;
    holder.colorAccount = colorAccount;
    TextView amount = v.findViewById(R.id.amount);
    UiUtils.configureAmountTextViewForHebrew(amount);
    holder.amount = amount;
    holder.category = v.findViewById(R.id.category);
    holder.color1 = v.findViewById(R.id.color1);
    holder.voidMarker = v.findViewById(R.id.voidMarker);
    TextView tv = v.findViewById(R.id.date);
    holder.date = tv;
    if (mAccount.getId() < 0) {
      colorAccount.setLayoutParams(
          new LayoutParams(4, LayoutParams.FILL_PARENT));
    }
    tv.setEms(dateEms);
    v.setTag(holder);
    return v;
  }

  @Override
  public void bindView(View view, Context context, Cursor cursor) {
    ViewHolder viewHolder = (ViewHolder) view.getTag();
    viewHolder.date.setText(Utils.convDateTime(cursor.getString(columnIndexDate), itemDateFormat));
    Currency currency = currencyIndexCurrency == -1 ? mAccount.currency : Currency.getInstance(cursor.getString(currencyIndexCurrency));
    long amount = cursor.getLong(columnIndexAmount);
    TextView tv1 = viewHolder.amount;
    tv1.setText(currencyFormatter.convAmount(amount, currency));

    tv1.setTextColor(amount < 0 ? colorExpense : colorIncome);
    if (mAccount.isAggregate()) {
      if (columnIndexSameCurrency == -1 || cursor.getInt(columnIndexSameCurrency) != 1) {
        int color = cursor.getInt(columnIndexColor);
        viewHolder.colorAccount.setBackgroundColor(color);
      } else {
        viewHolder.colorAccount.setBackgroundColor(0);
        tv1.setTextColor(textColorSecondary);
      }
    }
    TextView tv2 = viewHolder.category;
    CharSequence catText = cursor.getString(columnIndexLabelMain);
    if (DbUtils.getLongOrNull(cursor, columnIndexTransferPeer) != null) {
      catText = Transfer.getIndicatorPrefixForLabel(amount) + catText;
      if (mAccount.isAggregate()) {
        catText = cursor.getString(columnIndexAccountLabel) + " " + catText;
      }
    } else {
      Long catId = DbUtils.getLongOrNull(cursor, KEY_CATID);
      if (SPLIT_CATID.equals(catId))
        catText = MyApplication.getInstance().getString(R.string.split_transaction);
      else if (catId == null) {
        if (cursor.getInt(columnIndexStatus) != STATUS_HELPER) {
          catText = Category.NO_CATEGORY_ASSIGNED_LABEL;
        }
      } else {
        catText = getCatText(catText, cursor.getString(columnIndexLabelSub));
      }
    }
    String referenceNumber = cursor.getString(columnIndexReferenceNumber);
    if (referenceNumber != null && referenceNumber.length() > 0)
      catText = "(" + referenceNumber + ") " + catText;
    SpannableStringBuilder ssb;
    String comment = cursor.getString(columnIndexComment);
    if (comment != null && comment.length() > 0) {
      ssb = new SpannableStringBuilder(comment);
      ssb.setSpan(new StyleSpan(Typeface.ITALIC), 0, comment.length(), 0);
      catText = catText.length() > 0 ?
          TextUtils.concat(catText, TransactionList.COMMENT_SEPARATOR, ssb) :
          ssb;
    }
    String payee = cursor.getString(columnIndexPayee);
    if (payee != null && payee.length() > 0) {
      ssb = new SpannableStringBuilder(payee);
      ssb.setSpan(new UnderlineSpan(), 0, payee.length(), 0);
      catText = catText.length() > 0 ?
          TextUtils.concat(catText, TransactionList.COMMENT_SEPARATOR, ssb) :
          ssb;
    }
    if (insideFragment) {
      if (catText.length() == 0) {
        catText = "―――";
        tv2.setGravity(Gravity.CENTER);
      } else {
        tv2.setGravity(Gravity.START);
      }
    }
    tv2.setText(catText);

    CrStatus status;
    try {
      status = CrStatus.valueOf(cursor.getString(columnIndexCrStatus));
    } catch (IllegalArgumentException ex) {
      status = CrStatus.UNRECONCILED;
    }

    if (!mAccount.getType().equals(AccountType.CASH) && !status.equals(CrStatus.VOID)) {
      viewHolder.color1.setBackgroundColor(status.color);
      viewHolder.colorContainer.setTag(status == CrStatus.RECONCILED ? -1 : cursor.getLong(columnIndexRowId));
      viewHolder.colorContainer.setVisibility(View.VISIBLE);
    } else {
      viewHolder.colorContainer.setVisibility(View.GONE);
    }
    viewHolder.voidMarker.setVisibility(status.equals(CrStatus.VOID) ? View.VISIBLE : View.GONE);
  }

  /**
   * @param catText
   * @param label_sub
   * @return extracts the information that should
   * be displayed about the mapped category, can be overridden by subclass
   * should not be used for handle transfers
   */
  protected CharSequence getCatText(CharSequence catText, String label_sub) {
    if (label_sub != null && label_sub.length() > 0) {
      catText = catText + TransactionList.CATEGORY_SEPARATOR + label_sub;
    }
    return catText;
  }

  public void refreshDateFormat() {
    switch (mGroupingOverride != null ? mGroupingOverride : mAccount.getGrouping()) {
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

  @Override
  public Cursor swapCursor(Cursor cursor) {
    if (!indexesCalculated) {
      columnIndexDate = cursor.getColumnIndex(KEY_DATE);
      currencyIndexCurrency = cursor.getColumnIndex(KEY_CURRENCY);
      columnIndexAmount = cursor.getColumnIndex(KEY_AMOUNT);
      columnIndexSameCurrency = cursor.getColumnIndex(KEY_IS_SAME_CURRENCY);
      columnIndexColor = cursor.getColumnIndex(KEY_COLOR);
      columnIndexLabelMain = cursor.getColumnIndex(KEY_LABEL_MAIN);
      columnIndexTransferPeer = cursor.getColumnIndex(KEY_TRANSFER_PEER);
      columnIndexAccountLabel = cursor.getColumnIndex(KEY_ACCOUNT_LABEL);
      columnIndexStatus = cursor.getColumnIndex(KEY_STATUS);
      columnIndexLabelSub = cursor.getColumnIndex(KEY_LABEL_SUB);
      columnIndexReferenceNumber = cursor.getColumnIndex(KEY_REFERENCE_NUMBER);
      columnIndexComment = cursor.getColumnIndex(KEY_COMMENT);
      columnIndexPayee = cursor.getColumnIndex(KEY_PAYEE_NAME);
      columnIndexCrStatus = cursor.getColumnIndex(KEY_CR_STATUS);
      columnIndexRowId = cursor.getColumnIndex(KEY_ROWID);

      indexesCalculated = true;
    }
    return super.swapCursor(cursor);
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