package org.totschnig.myexpenses.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.annimon.stream.Stream;
import com.github.lzyzsd.circleprogress.DonutProgress;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.BudgetActivity;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.adapter.BudgetAdapter;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.viewmodel.data.Budget;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.totschnig.myexpenses.activity.BudgetActivity.getBackgroundForAvailable;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGETID;
import static org.totschnig.myexpenses.util.ColorUtils.getContrastColor;

public class BudgetFragment extends DistributionBaseFragment {
  private Budget budget;
  @BindView(R.id.budgetProgressTotal) DonutProgress budgetProgress;
  @BindView(R.id.totalBudget) TextView totalBudget;
  @BindView(R.id.totalAllocated) TextView totalAllocated;
  @BindView(R.id.totalAmount) TextView totalAmount;
  @BindView(R.id.totalAvailable) TextView totalAvailable;

  public long getAllocated() {
    return allocated;
  }

  private long allocated, spent;

  @Override
  protected boolean showAllCategories() {
    return true;
  }

  @Override
  protected Object getSecondarySort() {
    return KEY_BUDGET + " DESC";
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    setupAccount();
    if (mAccount == null) {
      return errorView();
    }
    View view = inflater.inflate(R.layout.budget_list, container, false);
    ButterKnife.bind(this, view);
    budgetProgress.setFinishedStrokeColor(mAccount.color);
    budgetProgress.setUnfinishedStrokeColor(getContrastColor(mAccount.color));
    totalBudget.setOnClickListener(view1 -> ((BudgetActivity) getActivity()).onBudgetClick(null, null));
    registerForContextMenu(mListView);
    return view;
  }

  public void setBudget(Budget budget) {
    final ActionBar actionBar = ((ProtectedFragmentActivity) getActivity()).getSupportActionBar();
    actionBar.setTitle(mAccount.getLabelForScreenTitle(getContext()));
    if (mAdapter == null) {
      mAdapter = new BudgetAdapter((BudgetActivity) getActivity(), currencyFormatter, budget.getCurrency());
      mListView.setAdapter(mAdapter);
    }
    if (this.budget == null || this.budget.getGrouping() != budget.getGrouping()) {
      mGrouping = budget.getGrouping();
      mGroupingYear = 0;
      mGroupingSecond = 0;
      updateDateInfo(false);
    } else {
      loadData();
    }
    this.budget = budget;
    updateTotals();
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
          mGroupingYear = thisYearOfWeekStart;
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
    totalAllocated.setText(currencyFormatter.formatCurrency(new Money(mAccount.getCurrencyUnit(),
        allocated)));
  }

  @Override
  void updateIncome(long amount) {

  }

  @Override
  void updateExpense(long amount) {
    this.spent = amount;
    updateTotals();
  }

  private void updateTotals() {
    final ProtectedFragmentActivity context = (ProtectedFragmentActivity) getActivity();
    if (context == null) {
      return;
    }
    totalBudget.setText(currencyFormatter.formatCurrency(budget.getAmount()));
    totalAmount.setText(currencyFormatter.formatCurrency(new Money(mAccount.getCurrencyUnit(), -spent)));
    final Long allocated = this.budget.getAmount().getAmountMinor();
    long available = allocated - spent;
    totalAvailable.setText(currencyFormatter.formatCurrency(new Money(mAccount.getCurrencyUnit(), available)));
    boolean onBudget = available >=0;
    totalAvailable.setBackgroundResource(getBackgroundForAvailable(onBudget, context.getThemeType()));
    totalAvailable.setTextColor(onBudget ? context.getColorIncome() :
        context.getColorExpense());
    int progress = allocated == 0 ? 100 : Math.round(spent * 100F / allocated);
    UiUtils.configureProgress(budgetProgress, progress);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    if (((BudgetActivity) getActivity()).hasBudgets()) {
      inflater.inflate(R.menu.budget, menu);
      super.onCreateOptionsMenu(menu, inflater);
    }
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    final MenuItem item = menu.findItem(R.id.GROUPING_COMMAND);
    if (item != null) {
      Utils.configureGroupingMenu(item.getSubMenu(), mGrouping);
    }
    super.onPrepareOptionsMenu(menu);
  }

  @Override
  protected String getExtraColumn() {
    return KEY_BUDGET;
  }

  @Override
  protected Uri getCategoriesUri() {
    return super.getCategoriesUri().buildUpon()
        .appendQueryParameter(KEY_BUDGETID, String.valueOf(budget.getId())).build();
  }

  @NonNull
  protected PrefKey getPrefKey() {
    return PrefKey.BUDGET_AGGREGATE_TYPES;
  }
}
