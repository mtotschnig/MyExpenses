package org.totschnig.myexpenses.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.annimon.stream.Stream;
import com.github.lzyzsd.circleprogress.DonutProgress;
import com.squareup.sqlbrite3.QueryObservable;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.BudgetActivity;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.adapter.BudgetAdapter;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Money;
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
import static org.totschnig.myexpenses.util.ColorUtils.getContrastColor;

public class BudgetFragment extends DistributionBaseFragment {
  private Budget budget;
  @BindView(R.id.budgetTotalCard) ViewGroup budgetTotalCard;
  @BindView(R.id.budgetProgressTotal) DonutProgress budgetProgress;
  @BindView(R.id.totalBudget) TextView totalBudget;
  @BindView(R.id.totalAllocated) TextView totalAllocated;
  @BindView(R.id.totalAmount) TextView totalAmount;
  @BindView(R.id.totalAvailable) TextView totalAvailable;

  public long getAllocated() {
    return allocated;
  }

  private long allocated;

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
    totalBudget.setOnClickListener(view1 -> ((BudgetActivity) getActivity()).onBudgetClick(null, null));
    registerForContextMenu(mListView);
    return view;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    // no search
  }

  public void setBudget(Budget budget) {
    final ActionBar actionBar = ((ProtectedFragmentActivity) getActivity()).getSupportActionBar();
    actionBar.setTitle(mAccount.getLabelForScreenTitle(getContext()));
    if (mAdapter == null) {
      mAdapter = new BudgetAdapter((BudgetActivity) getActivity(), currencyFormatter, budget.getCurrency());
      mListView.setAdapter(mAdapter);
    }
    if (this.budget == null || this.budget.getType() != budget.getType()) {
      mGrouping = budget.getType().toGrouping();
      mGroupingYear = 0;
      mGroupingSecond = 0;
      updateDateInfo(false);
    } else {
      loadData();
    }
    this.budget = budget;
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
      updateSum();
    } else {
      super.onDateInfoReceived(cursor);
      loadData();
    }
  }

  @Override
  protected void onLoadFinished() {
    super.onLoadFinished();
    allocated = Stream.of(mAdapter.getMainCategories()).mapToLong(category -> category.budget).sum();
    totalAllocated.setText(currencyFormatter.formatCurrency(new Money(mAccount.currency,
        allocated)));
  }

  @Override
  void updateIncome(Long amount) {

  }

  @Override
  void updateExpense(Long amount) {
    totalBudget.setText(currencyFormatter.formatCurrency(budget.getAmount()));
    totalAmount.setText(currencyFormatter.formatCurrency(new Money(mAccount.currency, -amount)));
    final Long allocated = this.budget.getAmount().getAmountMinor();
    long available = allocated - amount;
    totalAvailable.setText(currencyFormatter.formatCurrency(new Money(mAccount.currency, available)));
    boolean onBudget = available >=0;
    totalAvailable.setBackgroundResource(onBudget ? R.drawable.round_background_income :
        R.drawable.round_background_expense);
    totalAvailable.setTextColor(onBudget ? ((ProtectedFragmentActivity) getActivity()).getColorIncome() :
        ((ProtectedFragmentActivity) getActivity()).getColorExpense());
    int progress = available <= 0 || allocated == 0 ? 100 : Math.round(amount * 100F / allocated);
    budgetProgress.setProgress(progress);
    budgetProgress.setText(String.valueOf(progress));
    budgetProgress.setFinishedStrokeColor(mAccount.color);
    budgetProgress.setUnfinishedStrokeColor(getContrastColor(mAccount.color));
  }
}
