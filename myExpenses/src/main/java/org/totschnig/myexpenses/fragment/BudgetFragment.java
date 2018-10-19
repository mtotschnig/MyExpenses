package org.totschnig.myexpenses.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.sqlbrite3.QueryObservable;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.adapter.BudgetAdapter;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.viewmodel.data.Budget;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGETID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_COMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_EXTENDED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_VOID;

public class BudgetFragment extends DistributionBaseFragment {
  private Budget budget;
  private Account mAccount;
  @BindView(R.id.budgetTotalCard) ViewGroup budgetTotalCard;

  @Override
  protected QueryObservable createQuery() {
    String accountSelector = null, sortOrder;
    String[] selectionArgs, projection;
    String catFilter;
    String accountSelection, amountCalculation = KEY_AMOUNT, table = VIEW_COMMITTED;
    if (budget.isHomeAggregate()) {
      accountSelection = null;
      amountCalculation = DatabaseConstants.getAmountHomeEquivalent();
      table = VIEW_EXTENDED;
    } else if (budget.isAggregate()) {
      accountSelection = " IN " +
          "(SELECT " + KEY_ROWID + " from " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY + " = ? AND " +
          KEY_EXCLUDE_FROM_TOTALS + " = 0 )";
      accountSelector = budget.getCurrency().getCurrencyCode();
    } else {
      accountSelection = " = " + budget.getAccountId();
    }
    catFilter = "FROM " + table +
        " WHERE " + WHERE_NOT_VOID + (accountSelection == null ? "" : (" AND +" + KEY_ACCOUNTID + accountSelection));
    catFilter += " AND " + KEY_AMOUNT + " < 0";
    catFilter += " AND " + buildGroupingClause();
    //we need to include transactions mapped to children for main categories
    catFilter += " AND " + CATTREE_WHERE_CLAUSE;
    projection = new String[]{
        KEY_ROWID,
        KEY_PARENTID,
        KEY_LABEL,
        KEY_COLOR,
        "(SELECT sum(" + amountCalculation + ") " + catFilter + ") AS " + KEY_SUM,
        KEY_AMOUNT //Budget
    };
    sortOrder = "abs(" + KEY_SUM + ") DESC";
    selectionArgs = accountSelector != null ? new String[]{accountSelector} : null;
    return briteContentResolver.createQuery(TransactionProvider.CATEGORIES_URI.buildUpon()
            .appendQueryParameter(KEY_BUDGETID, String.valueOf(budget.getId())).build(),
        projection, null, selectionArgs, sortOrder, true);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    long accountId = getActivity().getIntent().getLongExtra(KEY_ACCOUNTID, 0);
    mAccount = Account.getInstanceFromDb(accountId);
    if (mAccount == null) {
      TextView tv = new TextView(getContext());
      //noinspection SetTextI18n
      tv.setText("Error loading budget for account " + accountId);
      return tv;
    }
    View view = inflater.inflate(R.layout.budget_list, container, false);
    ButterKnife.bind(this, view);
    return view;
  }

  public void setBudget(Budget budget) {
    final ActionBar actionBar = ((ProtectedFragmentActivity) getActivity()).getSupportActionBar();
    actionBar.setTitle(mAccount.getLabelForScreenTitle(getContext()));
    actionBar.setSubtitle(budget.getType().getLabel(getActivity()));
    ((TextView) budgetTotalCard.findViewById(R.id.budget)).setText(currencyFormatter.formatCurrency(budget.getAmount()));
    ((TextView) budgetTotalCard.findViewById(R.id.amount)).setText(currencyFormatter.formatCurrency(budget.getAmount()));
    final TextView availableTV = budgetTotalCard.findViewById(R.id.available);
    availableTV.setText(currencyFormatter.formatCurrency(budget.getAmount()));
    boolean onBudget = true;
    availableTV.setBackgroundResource(onBudget ? R.drawable.round_background_income :
        R.drawable.round_background_expense);
    availableTV.setTextColor(onBudget ? ((ProtectedFragmentActivity) getActivity()).getColorIncome() :
        ((ProtectedFragmentActivity) getActivity()).getColorExpense());
    this.budget = budget;
    mGrouping = budget.getType().toGrouping();
    final ProtectedFragmentActivity ctx = (ProtectedFragmentActivity) getActivity();
    mAdapter = new BudgetAdapter(ctx, currencyFormatter, budget.getCurrency(), true,
        true);
    mListView.setAdapter(mAdapter);
    updateDateInfo(false);
  }

  @Override
  protected void onDateInfoReceived(Cursor cursor) {
    //we fetch dateinfo from database two times, first to get info about current date,
    //then we use this info in second run
    if (mGroupingYear == 0) {
      mGroupingYear = thisYear;
      switch(mGrouping) {
        case DAY:
          mGroupingSecond = thisDay;
          break;
        case WEEK:
          mGroupingSecond = thisWeek;
          break;
        case MONTH:
          mGroupingSecond = thisMonth;
          break;
      }
      updateDateInfo(true);
    } else {
      loadData();
    }
  }
}
