package org.totschnig.myexpenses.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageCategories;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.CrStatus;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.cursoradapter.widget.ResourceCursorAdapter;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.totschnig.myexpenses.preference.PrefKey.CRITERION_FUTURE;
import static org.totschnig.myexpenses.preference.PrefKey.GROUP_MONTH_STARTS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_TYPE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_SAME_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_MAIN;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_SUB;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGLIST;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_HELPER;

public class TransactionAdapter extends ResourceCursorAdapter {
  private int dateEms;
  private boolean is24HourFormat;
  private boolean shouldShowTime;
  private Account mAccount;
  private Grouping mGroupingOverride;
  private DateFormat localizedTimeFormat, itemDateFormat;
  private int colorExpense, colorIncome;
  private ColorStateList textColorSecondary;
  private boolean insideFragment;
  private int monthStart;
  private CurrencyFormatter currencyFormatter;
  private boolean indexesCalculated = false;
  private int columnIndexDate;
  private int columnIndexCurrency;
  private int columnIndexSameCurrency;
  private int columnIndexColor;
  private int columnIndexLabelMain;
  private int columnIndexAccountLabel;
  private int columIndexAccountType;
  private int columnIndexStatus;
  private int columnIndexLabelSub;
  private int columnIndexReferenceNumber;
  private int columnIndexComment;
  private int columnIndexPayee;
  private int columnIndexCrStatus;
  private int columnIndexRowId;
  private int columnIndexAmount;
  private int columnIndexTransferPeer;
  private int columnIndexEquivalentAmount;
  private int columnIndexTagList;

  private CurrencyContext currencyContext;
  private PrefHandler prefHandler;

  private Context context;

  private long futureCriterion;

  protected TransactionAdapter(Grouping grouping, Context context, int layout,
                               Cursor c, int flags, CurrencyFormatter currencyFormatter,
                               PrefHandler prefHandler, CurrencyContext currencyContext) {
    super(context, layout, c, flags);
    if (context instanceof ManageCategories) {
      insideFragment = true;
    }
    this.context = context;
    colorIncome = context.getResources().getColor(R.color.colorIncome);
    colorExpense = context.getResources().getColor(R.color.colorExpense);
    textColorSecondary = ((ProtectedFragmentActivity) context).getTextColorSecondary();
    mGroupingOverride = grouping;
    is24HourFormat = android.text.format.DateFormat.is24HourFormat(context);
    localizedTimeFormat = android.text.format.DateFormat.getTimeFormat(context);
    this.currencyFormatter = currencyFormatter;
    this.prefHandler = prefHandler;
    monthStart = Integer.parseInt(prefHandler.getString(GROUP_MONTH_STARTS, "1"));
    this.currencyContext = currencyContext;
  }

  public TransactionAdapter(Context context, int layout, Cursor c, int flags,
                            CurrencyFormatter currencyFormatter, PrefHandler prefHandler,
                            CurrencyContext currencyContext) {
    this(null, context, layout, c, flags, currencyFormatter, prefHandler, currencyContext);
  }

  @Override
  public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v = super.newView(context, cursor, parent);
    ViewHolder holder = new ViewHolder(v);
    if (mAccount.getId() < 0) {
      holder.colorAccount.setVisibility(View.VISIBLE);
    }
    v.setTag(holder);
    return v;
  }

  @Override
  public void bindView(View view, Context context, Cursor cursor) {
    ViewHolder viewHolder = (ViewHolder) view.getTag();
    if (viewHolder == null) {
      return;
    }
    viewHolder.date.setEms(dateEms);
    final long date = cursor.getLong(columnIndexDate);
    ((FrameLayout) view).setForeground(date  > futureCriterion ? new ColorDrawable(context.getResources().getColor(R.color.future_background)) : null);
    viewHolder.date.setText(itemDateFormat != null ?
        Utils.convDateTime(date, itemDateFormat) : null);
    final boolean isTransfer = DbUtils.getLongOrNull(cursor, columnIndexTransferPeer) != null;
    //for the Grand Total account, we show equivalent amounts in the home currency for normal transactions
    //but show transfers in there real currency
    CurrencyUnit currency = isTransfer && columnIndexCurrency > -1 ?
        currencyContext.get(cursor.getString(columnIndexCurrency)) : mAccount.getCurrencyUnit();
    long amount = cursor.getLong(isTransfer || columnIndexEquivalentAmount == -1 ?
        columnIndexAmount : columnIndexEquivalentAmount);
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
    CharSequence catText = DbUtils.getString(cursor, columnIndexLabelMain);
    if (isTransfer) {
      catText = Transfer.getIndicatorPrefixForLabel(amount) + catText;
      if (mAccount.isAggregate()) {
        catText = cursor.getString(columnIndexAccountLabel) + " " + catText;
      }
    } else {
      Long catId = DbUtils.getLongOrNull(cursor, KEY_CATID);
      if (SPLIT_CATID.equals(catId))
        catText = context.getString(R.string.split_transaction);
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
    String tagList = cursor.getString(columnIndexTagList);
    if (tagList != null && tagList.length() > 0) {
      ssb = new SpannableStringBuilder(tagList);
      ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, tagList.length(), 0);
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

    if (!cursor.getString(columIndexAccountType).equals(AccountType.CASH.name()) && !status.equals(CrStatus.VOID)) {
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
  protected CharSequence getCatText(CharSequence catText, @Nullable String label_sub) {
    if (label_sub != null && label_sub.length() > 0) {
      catText = catText + TransactionList.CATEGORY_SEPARATOR + label_sub;
    }
    return catText;
  }

  private Locale localeFromContext() {
    return Utils.localeFromContext(context);
  }

  public void refreshDateFormat() {
    dateEms = 3;
    switch (mGroupingOverride != null ? mGroupingOverride : mAccount.getGrouping()) {
      case DAY:
        if (shouldShowTime) {
          itemDateFormat = localizedTimeFormat;
          dateEms = is24HourFormat ? 3 : 4;
        } else {
          itemDateFormat = null;
          dateEms = 0;
        }
        break;
      case MONTH:
        //noinspection SimpleDateFormat
        if (monthStart == 1) {
          itemDateFormat = new SimpleDateFormat("dd", localeFromContext());
          dateEms = 2;
        } else {
          itemDateFormat = Utils.localizedYearLessDateFormat(context);
        }
        break;
      case WEEK:
        //noinspection SimpleDateFormat
        dateEms = 2;
        itemDateFormat = new SimpleDateFormat("EEE", localeFromContext());
        break;
      case YEAR:
        itemDateFormat = Utils.localizedYearLessDateFormat(context);
        break;
      case NONE:
        itemDateFormat = Utils.ensureDateFormatWithShortYear(context);
        dateEms = 4;
    }
  }

  @Override
  public Cursor swapCursor(Cursor cursor) {
    futureCriterion = "current".equals(prefHandler.getString(CRITERION_FUTURE, "end_of_day")) ? System.currentTimeMillis() / 1000 :  LocalDate.now().plusDays( 1 ).atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond();
    if (!indexesCalculated) {
      columnIndexDate = cursor.getColumnIndex(KEY_DATE);
      columnIndexCurrency = cursor.getColumnIndex(KEY_CURRENCY);
      columnIndexAmount = cursor.getColumnIndex(KEY_AMOUNT);
      columnIndexSameCurrency = cursor.getColumnIndex(KEY_IS_SAME_CURRENCY);
      columnIndexColor = cursor.getColumnIndex(KEY_COLOR);
      columnIndexLabelMain = cursor.getColumnIndex(KEY_LABEL_MAIN);
      columnIndexTransferPeer = cursor.getColumnIndex(KEY_TRANSFER_PEER);
      columnIndexAccountLabel = cursor.getColumnIndex(KEY_ACCOUNT_LABEL);
      columIndexAccountType = cursor.getColumnIndex(KEY_ACCOUNT_TYPE);
      columnIndexStatus = cursor.getColumnIndex(KEY_STATUS);
      columnIndexLabelSub = cursor.getColumnIndex(KEY_LABEL_SUB);
      columnIndexReferenceNumber = cursor.getColumnIndex(KEY_REFERENCE_NUMBER);
      columnIndexComment = cursor.getColumnIndex(KEY_COMMENT);
      columnIndexPayee = cursor.getColumnIndex(KEY_PAYEE_NAME);
      columnIndexCrStatus = cursor.getColumnIndex(KEY_CR_STATUS);
      columnIndexRowId = cursor.getColumnIndex(KEY_ROWID);
      columnIndexEquivalentAmount = cursor.getColumnIndex(KEY_EQUIVALENT_AMOUNT);
      columnIndexTagList = cursor.getColumnIndex(KEY_TAGLIST);

      indexesCalculated = true;
    }
    return super.swapCursor(cursor);
  }

  public void setAccount(Account account) {
    this.mAccount = account;
    shouldShowTime = UiUtils.getDateMode(account.getType(), prefHandler) == UiUtils.DateMode.DATE_TIME;
    refreshDateFormat();
  }

  class ViewHolder {
    @BindView(R.id.amount) TextView amount;
    @BindView(R.id.colorAccount) View colorAccount;
    @BindView(R.id.category) TextView category;
    @BindView(R.id.color1) View color1;
    @BindView(R.id.colorContainer) View colorContainer;
    @BindView(R.id.date) TextView date;
    @BindView(R.id.voidMarker) View voidMarker;

    ViewHolder(View view) {
      ButterKnife.bind(this, view);
    }


    @OnClick(R.id.colorContainer)
    void toggleCrStatus(View v) {
      Long id = (Long) v.getTag();
      if (id != -1 && !mAccount.isSealed()) {
        ((ProtectedFragmentActivity) context).startTaskExecution(
            TaskExecutionFragment.TASK_TOGGLE_CRSTATUS,
            new Long[]{id},
            null,
            0);
      }
    }
  }

}