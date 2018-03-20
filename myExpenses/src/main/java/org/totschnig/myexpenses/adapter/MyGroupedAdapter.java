package org.totschnig.myexpenses.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v4.widget.ResourceCursorAdapter;
import android.support.v7.widget.CardView;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.model.AccountGrouping;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.AggregateAccount;
import org.totschnig.myexpenses.model.CurrencyEnum;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
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
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_KEY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_INCOME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_TRANSFERS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TOTAL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;

public class MyGroupedAdapter extends ResourceCursorAdapter implements StickyListHeadersAdapter {
  private static final int CARD_ELEVATION_DIP = 24;
  private final CurrencyFormatter currencyFormatter;

  private AccountGrouping grouping;
  private long highlightedAccountId;
  LayoutInflater inflater;
  private ProtectedFragmentActivity activity;

  public MyGroupedAdapter(ProtectedFragmentActivity context, int layout, Cursor c,
                          CurrencyFormatter currencyFormatter) {
    super(context, layout, c, 0);
    inflater = LayoutInflater.from(context);
    this.currencyFormatter = currencyFormatter;
    this.activity = context;
  }

  public void setGrouping(AccountGrouping grouping) {
    this.grouping = grouping;
  }

  public void setHighlightedAccountId(long accountId) {
    this.highlightedAccountId = accountId;
    notifyDataSetChanged();
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
      headerText = activity.getString(R.string.grand_total);
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
    int aggregate = c.getInt(c.getColumnIndexOrThrow(KEY_IS_AGGREGATE));
    if (aggregate == AggregateAccount.AGGREGATE_HOME) {
      return Long.MAX_VALUE;
    }
    switch (grouping) {
      case CURRENCY:
        return CurrencyEnum.valueOf(c.getString(c.getColumnIndex(KEY_CURRENCY))).ordinal();
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

    Currency currency = Utils.getSaveInstance(cursor.getString(cursor.getColumnIndex(KEY_CURRENCY)));
    final long rowId = cursor.getLong(cursor.getColumnIndex(KEY_ROWID));
    long sum_transfer = cursor.getLong(cursor.getColumnIndex(KEY_SUM_TRANSFERS));

    boolean isHighlighted = rowId == highlightedAccountId;
    boolean has_future = cursor.getInt(cursor.getColumnIndex(KEY_HAS_FUTURE)) > 0;
    final int isAggregate = cursor.getInt(cursor.getColumnIndex(KEY_IS_AGGREGATE));
    final int count = cursor.getCount();
    boolean hide_cr;
    int colorInt;

    holder.card.setCardElevation(isHighlighted ? TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, CARD_ELEVATION_DIP, context.getResources().getDisplayMetrics()) :
        0);
    holder.label.setTypeface(
        Typeface.create(holder.label.getTypeface(), Typeface.NORMAL),
        isHighlighted ? Typeface.BOLD : Typeface.NORMAL);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      holder.selectedIndicator.setVisibility(isHighlighted ? View.VISIBLE : View.GONE);
    }
    if (isAggregate > 0) {
      holder.accountMenu.setVisibility(View.INVISIBLE);
      holder.accountMenu.setOnClickListener(null);
    } else {
      holder.accountMenu.setVisibility(View.VISIBLE);
      boolean upVisible = false, downVisible = false;
      int position = cursor.getPosition();
      if (PrefKey.SORT_ORDER_ACCOUNTS.getString(ProtectedFragmentActivity.SORT_ORDER_USAGES).equals(ProtectedFragmentActivity.SORT_ORDER_CUSTOM)) {
        if (position > 0 && getHeaderId(position - 1) == getHeaderId(position)) {
          getCursor().moveToPosition(position - 1);
          if (rowId > 0) upVisible = true; //ignore if previous is aggregate
        }
        if (position + 1 < getCount() && getHeaderId(position + 1) == getHeaderId(position)) {
          getCursor().moveToPosition(position + 1);
          if (rowId > 0) downVisible = true;
        }
        getCursor().moveToPosition(position);
      }
      final boolean finalUpVisible = upVisible, finalDownVisible = downVisible;
      holder.accountMenu.setOnClickListener(v1 -> {
        PopupMenu popup = new PopupMenu(context, holder.accountMenu);
        popup.inflate(R.menu.accounts_context);
        Menu menu = popup.getMenu();
        menu.findItem(R.id.DELETE_ACCOUNT_COMMAND).setVisible(count > 1);
        menu.findItem(R.id.UP_COMMAND).setVisible(finalUpVisible);
        menu.findItem(R.id.DOWN_COMMAND).setVisible(finalDownVisible);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

          @Override
          public boolean onMenuItemClick(MenuItem item) {
            return handleSwap(item.getItemId(), position) ||
                activity.dispatchCommand(item.getItemId(), rowId);
          }

          private boolean handleSwap(int itemId, int position1) {
            if (itemId != R.id.UP_COMMAND && itemId != R.id.DOWN_COMMAND) return false;
            Cursor c1 = getCursor();
            c1.moveToPosition(position1);
            String sortKey1 = c1.getString(c1.getColumnIndex(KEY_SORT_KEY));
            c1.moveToPosition(itemId == R.id.UP_COMMAND ? position1 - 1 : position1 + 1);
            String sortKey2 = c1.getString(c1.getColumnIndex(KEY_SORT_KEY));
            activity.startTaskExecution(
                TaskExecutionFragment.TASK_SWAP_SORT_KEY,
                new String[]{sortKey1, sortKey2},
                null,
                R.string.progress_dialog_saving);
            return true;
          }
        });
        popup.show();
      });
    }

    final boolean isHome = isAggregate == AggregateAccount.AGGREGATE_HOME;
    holder.label.setVisibility(isHome ? View.GONE : View.VISIBLE);

    if (isAggregate > 0) {
      hide_cr = true;
      if (grouping == AccountGrouping.CURRENCY) {
        holder.label.setText(R.string.menu_aggregates);
      }
      colorInt = activity.getColorAggregate();
    } else {
      //for deleting we need the position, because we need to find out the account's label
      try {
        hide_cr = AccountType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(KEY_TYPE))).equals(AccountType.CASH);
      } catch (IllegalArgumentException ex) {
        hide_cr = true;
      }
      colorInt = cursor.getInt(cursor.getColumnIndex(KEY_COLOR));
    }
    holder.transferRow.setVisibility(sum_transfer == 0 ? View.GONE : View.VISIBLE);
    holder.totalRow.setVisibility(has_future ? View.VISIBLE : View.GONE);
    holder.clearedRow.setVisibility(hide_cr ? View.GONE : View.VISIBLE);
    holder.reconciledRow.setVisibility(hide_cr ? View.GONE : View.VISIBLE);
    if (sum_transfer != 0) {
      setConvertedAmount(holder.sumTransfer, currency, sum_transfer, isHome);
    }
    holder.color1.setBackgroundColor(colorInt);
    setConvertedAmount(holder.openingBalance, currency, cursor, KEY_OPENING_BALANCE, isHome);
    setConvertedAmount(holder.sumIncome, currency, cursor, KEY_SUM_INCOME, isHome);
    setConvertedAmount(holder.sumExpenses, currency, cursor, KEY_SUM_EXPENSES, isHome);
    setConvertedAmount(holder.currentBalance, currency, cursor, KEY_CURRENT_BALANCE, isHome);
    setConvertedAmount(holder.total, currency, cursor, KEY_TOTAL, isHome);
    setConvertedAmount(holder.reconciledTotal, currency, cursor, KEY_RECONCILED_TOTAL, isHome);
    setConvertedAmount(holder.clearedTotal, currency, cursor, KEY_CLEARED_TOTAL, isHome);
    String description = cursor.getString(cursor.getColumnIndex(KEY_DESCRIPTION));
    if (TextUtils.isEmpty(description)) {
      holder.description.setVisibility(View.GONE);
    } else {
      holder.description.setText(description);
      holder.description.setVisibility(View.VISIBLE);
    }
    holder.label.setText( cursor.getString(cursor.getColumnIndex(KEY_LABEL)));
  }

  private void setConvertedAmount(TextView tv, Currency currency, Cursor c, String columnName, boolean isHome) {
    tv.setText(String.format(Locale.getDefault(),"%s%s", isHome ? " ≈ " : "",
        currencyFormatter.convAmount(c.getLong(c.getColumnIndex(columnName)), currency)));
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
    @BindView(R.id.color1) View color1;
    @BindView(R.id.account_menu) View accountMenu;
    @BindView(R.id.card) CardView card;
    @BindView(R.id.selected_indicator) View selectedIndicator;
    @BindView(R.id.TransferRow) View transferRow;
    @BindView(R.id.TotalRow) View totalRow;
    @BindView(R.id.ClearedRow) View clearedRow;
    @BindView(R.id.ReconciledRow) View reconciledRow;
    @BindView(R.id.sum_transfer) TextView sumTransfer;
    @BindView(R.id.opening_balance) TextView openingBalance;
    @BindView(R.id.sum_income) TextView sumIncome;
    @BindView(R.id.sum_expenses) TextView sumExpenses;
    @BindView(R.id.current_balance) TextView currentBalance;
    @BindView(R.id.total) TextView total;
    @BindView(R.id.reconciled_total) TextView reconciledTotal;
    @BindView(R.id.cleared_total) TextView clearedTotal;
    @BindView(R.id.description) TextView description;
    @BindView(R.id.label) TextView label;
 


    ViewHolder(View view) {
      ButterKnife.bind(this, view);
    }
  }
}
