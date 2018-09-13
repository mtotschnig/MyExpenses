package org.totschnig.myexpenses.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.widget.ResourceCursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.model.AccountGrouping;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.AggregateAccount;
import org.totschnig.myexpenses.model.CurrencyEnum;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.ui.ExpansionPanel;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.Utils;

import java.util.Currency;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CLEARED_TOTAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENT_BALANCE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HAS_FUTURE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_AGGREGATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_OPENING_BALANCE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_RECONCILED_TOTAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_INCOME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_TRANSFERS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TOTAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;

public class MyGroupedAdapter extends ResourceCursorAdapter implements StickyListHeadersAdapter {
  private final static String EXPANSION_PREF_PREFIX = "ACCOUNT_EXPANSION_";

  private final CurrencyFormatter currencyFormatter;
  private AccountGrouping grouping;
  private LayoutInflater inflater;
  private ProtectedFragmentActivity activity;
  private PrefHandler prefHandler;

  public MyGroupedAdapter(ProtectedFragmentActivity context, Cursor c,
                          CurrencyFormatter currencyFormatter, PrefHandler prefHandler) {
    super(context, R.layout.account_row_ng, c, 0);
    inflater = LayoutInflater.from(context);
    this.currencyFormatter = currencyFormatter;
    this.activity = context;
    this.prefHandler = prefHandler;
  }

  public void setGrouping(AccountGrouping grouping) {
    this.grouping = grouping;
  }

  @Override
  public View getHeaderView(int position, View convertView, ViewGroup parent) {
    HeaderViewHolder holder;
    if (convertView == null) {
      convertView = inflater.inflate(R.layout.accounts_header, parent, false);
      holder = new HeaderViewHolder(convertView);
      convertView.setTag(holder);
    } else {
      holder = (HeaderViewHolder) convertView.getTag();
    }
    Cursor c = getCursor();
    c.moveToPosition(position);
    long headerId = getHeaderId(position);
    String headerText = null;
    if (headerId == Long.MAX_VALUE) {
      headerText = activity.getString(R.string.menu_aggregates);
    } else {
      switch (grouping) {
        case CURRENCY:
          headerText = CurrencyEnum.valueOf(c.getString(c.getColumnIndex(KEY_CURRENCY))).toString();
          break;
        case NONE:
          headerText = activity.getString(headerId == 0 ? R.string.pref_manage_accounts_title : R.string.menu_aggregates);
          break;
        case TYPE:
          int headerRes;
          if (headerId == AccountType.values().length) {
            headerRes = R.string.menu_aggregates;
          } else {
            headerRes = AccountType.values()[(int) headerId].toStringResPlural();
          }
          headerText = activity.getString(headerRes);
          break;
      }
    }
    holder.sectionLabel.setText(headerText);
    return convertView;
  }

  @Override
  public long getHeaderId(int position) {
    Cursor c = getCursor();
    c.moveToPosition(position);
    switch (grouping) {
      case CURRENCY:
        return c.getInt(c.getColumnIndexOrThrow(KEY_IS_AGGREGATE)) == AggregateAccount.AGGREGATE_HOME ?
            Long.MAX_VALUE : CurrencyEnum.valueOf(c.getString(c.getColumnIndex(KEY_CURRENCY))).ordinal();
      case NONE:
        return c.getLong(c.getColumnIndex(KEY_ROWID)) > 0 ? 0 : 1;
      case TYPE:
        AccountType type;
        try {
          type = AccountType.valueOf(c.getString(c.getColumnIndexOrThrow(KEY_TYPE)));
          return type.ordinal();
        } catch (IllegalArgumentException ex) {
          return AccountType.values().length;
        }
    }
    return 0;
  }

  @Override
  public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v = super.newView(context, cursor, parent);
    ViewHolder holder = new ViewHolder(v);
    v.setTag(holder);
    return v;
  }

  @Override
  public void bindView(View view, Context context, Cursor cursor) {
    ViewHolder holder = ((ViewHolder) view.getTag());

    long id = cursor.getLong(cursor.getColumnIndex(KEY_ROWID));

    Currency currency = Utils.getSaveInstance(cursor.getString(cursor.getColumnIndex(KEY_CURRENCY)));
    long sum_transfer = cursor.getLong(cursor.getColumnIndex(KEY_SUM_TRANSFERS));

    boolean has_future = cursor.getInt(cursor.getColumnIndex(KEY_HAS_FUTURE)) > 0;
    final int isAggregate = cursor.getInt(cursor.getColumnIndex(KEY_IS_AGGREGATE));
    final String label = cursor.getString(cursor.getColumnIndex(KEY_LABEL));
    boolean hide_cr;
    int colorInt;
    String expansionPrefKey;

    final boolean isHome = isAggregate == AggregateAccount.AGGREGATE_HOME;

    if (isAggregate > 0) {
      hide_cr = true;
      if (isHome) {
        holder.label.setText(R.string.grand_total);
      } else if (grouping == AccountGrouping.CURRENCY) {
        holder.label.setText(R.string.menu_aggregates);
      } else {
        holder.label.setText(label);
      }
      colorInt = activity.getColorAggregate();
      expansionPrefKey = String.format(Locale.ROOT, "%s%s", EXPANSION_PREF_PREFIX,
          isHome ? AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE : currency.getCurrencyCode());
      holder.colorAccount.setImageResource(R.drawable.ic_action_equal_white);
    } else {
      holder.label.setText(label);
      //for deleting we need the position, because we need to find out the account's label
      try {
        hide_cr = AccountType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(KEY_TYPE))).equals(AccountType.CASH);
      } catch (IllegalArgumentException ex) {
        hide_cr = true;
      }
      colorInt = cursor.getInt(cursor.getColumnIndex(KEY_COLOR));
      expansionPrefKey = String.format(Locale.ROOT, "%s%d", EXPANSION_PREF_PREFIX, id);
      holder.colorAccount.setImageDrawable(null);
    }
    final boolean isExpanded = prefHandler.getBoolean(expansionPrefKey, true);
    holder.transferRow.setVisibility(sum_transfer == 0 ? View.GONE : View.VISIBLE);
    holder.totalRow.setVisibility(has_future ? View.VISIBLE : View.GONE);
    holder.clearedRow.setVisibility(hide_cr ? View.GONE : View.VISIBLE);
    holder.reconciledRow.setVisibility(hide_cr ? View.GONE : View.VISIBLE);
    if (sum_transfer != 0) {
      setConvertedAmount(holder.sumTransfer, currency, sum_transfer, isHome);
    }
    holder.colorAccount.setBackgroundDrawable(createBackgroundColorDrawable(colorInt));
    setConvertedAmount(currency, cursor, KEY_OPENING_BALANCE, isHome, holder.openingBalance);
    setConvertedAmount(currency, cursor, KEY_SUM_INCOME, isHome, holder.sumIncome);
    setConvertedAmount(currency, cursor, KEY_SUM_EXPENSES, isHome, holder.sumExpenses);
    setConvertedAmount(currency, cursor, KEY_CURRENT_BALANCE, isHome, holder.currentBalance, holder.currentBalanceHeader);
    setBalanceVisibility(holder, isExpanded);
    setConvertedAmount(currency, cursor, KEY_TOTAL, isHome, holder.total);
    setConvertedAmount(currency, cursor, KEY_RECONCILED_TOTAL, isHome, holder.reconciledTotal);
    setConvertedAmount(currency, cursor, KEY_CLEARED_TOTAL, isHome, holder.clearedTotal);
    String description = cursor.getString(cursor.getColumnIndex(KEY_DESCRIPTION));
    if (TextUtils.isEmpty(description)) {
      holder.description.setVisibility(View.GONE);
    } else {
      holder.description.setText(description);
      holder.description.setVisibility(View.VISIBLE);
    }

    holder.expansionPanel.setContentVisibility(isExpanded ? View.VISIBLE : View.GONE);
    holder.expansionPanel.setListener(expanded -> {
      prefHandler.putBoolean(expansionPrefKey, expanded);
      setBalanceVisibility(holder, expanded);
    });
  }

  private void setBalanceVisibility(ViewHolder holder, boolean expanded) {
    holder.currentBalanceHeader.setVisibility(expanded ? View.GONE : View.VISIBLE);
    holder.currentBalance.setVisibility(expanded ? View.VISIBLE: View.INVISIBLE);
  }

  private void setConvertedAmount(Currency currency, Cursor c, String columnName, boolean isHome, TextView ... tvs) {
    final String result = String.format(Locale.getDefault(), "%s%s", isHome ? " ≈ " : "",
        currencyFormatter.convAmount(c.getLong(c.getColumnIndex(columnName)), currency));
    for (TextView tv: tvs) {
      tv.setText(result);
    }
  }
  private void setConvertedAmount(TextView tv, Currency currency, long value, boolean isHome) {
    tv.setText(String.format(Locale.getDefault(),"%s%s", isHome ? " ≈ " : "",
        currencyFormatter.convAmount(value, currency)));
  }

  class HeaderViewHolder {
    @BindView(R.id.sectionLabel) TextView sectionLabel;

    HeaderViewHolder(View view) {
      ButterKnife.bind(this, view);
    }
  }

  class ViewHolder {
    @BindView(R.id.expansionPanel) ExpansionPanel expansionPanel;
    @BindView(R.id.colorAccount) ImageView colorAccount;
    @BindView(R.id.TransferRow) View transferRow;
    @BindView(R.id.TotalRow) View totalRow;
    @BindView(R.id.ClearedRow) View clearedRow;
    @BindView(R.id.ReconciledRow) View reconciledRow;
    @BindView(R.id.sum_transfer) TextView sumTransfer;
    @BindView(R.id.opening_balance) TextView openingBalance;
    @BindView(R.id.sum_income) TextView sumIncome;
    @BindView(R.id.sum_expenses) TextView sumExpenses;
    @BindView(R.id.current_balance) TextView currentBalance;
    @BindView(R.id.current_balance_header) TextView currentBalanceHeader;
    @BindView(R.id.total) TextView total;
    @BindView(R.id.reconciled_total) TextView reconciledTotal;
    @BindView(R.id.cleared_total) TextView clearedTotal;
    @BindView(R.id.description) TextView description;
    @BindView(R.id.label) TextView label;

    ViewHolder(View view) {
      ButterKnife.bind(this, view);
    }
  }

  private Drawable createBackgroundColorDrawable(int color) {
    GradientDrawable mask = new GradientDrawable();
    mask.setShape(GradientDrawable.OVAL);
    mask.setColor(color);
    return mask;
  }
}
