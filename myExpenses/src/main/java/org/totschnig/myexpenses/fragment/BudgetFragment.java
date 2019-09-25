package org.totschnig.myexpenses.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.annimon.stream.Stream;
import com.github.lzyzsd.circleprogress.DonutProgress;
import com.google.android.material.snackbar.Snackbar;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.adapter.BudgetAdapter;
import org.totschnig.myexpenses.model.AggregateAccount;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.TextUtils;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.viewmodel.BudgetViewModel;
import org.totschnig.myexpenses.viewmodel.data.Budget;
import org.totschnig.myexpenses.viewmodel.data.Category;

import java.math.BigDecimal;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.ButterKnife;
import eltos.simpledialogfragment.form.AmountEdit;
import eltos.simpledialogfragment.form.SimpleFormDialog;

import static org.totschnig.myexpenses.activity.BudgetActivity.getBackgroundForAvailable;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGETID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.util.ColorUtils.getContrastColor;
import static org.totschnig.myexpenses.util.TextUtils.appendCurrencySymbol;

public class BudgetFragment extends DistributionBaseFragment implements
    BudgetAdapter.OnBudgetClickListener, SimpleFormDialog.OnDialogResultListener {
  private Budget budget;
  @NonNull
  private CurrencyUnit currencyUnit;
  @BindView(R.id.budgetProgressTotal) DonutProgress budgetProgress;
  @BindView(R.id.totalBudget) TextView totalBudget;
  @BindView(R.id.totalAllocated) TextView totalAllocated;
  @BindView(R.id.totalAmount) TextView totalAmount;
  @BindView(R.id.totalAvailable) TextView totalAvailable;
  private static final String EDIT_BUDGET_DIALOG = "EDIT_BUDGET";

  private BudgetViewModel viewModel;

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
    View view = inflater.inflate(R.layout.budget_list, container, false);
    ButterKnife.bind(this, view);
    totalBudget.setOnClickListener(view1 -> onBudgetClick(null, null));
    registerForContextMenu(mListView);
    return view;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    viewModel = ViewModelProviders.of(this).get(BudgetViewModel.class);
    viewModel.getBudget().observe(this, this::setBudget);
  }

  private void showEditBudgetDialog(Category category, Category parentItem) {
    final Money amount, max, min;
    final SimpleFormDialog simpleFormDialog = new SimpleFormDialog()
        .title(category == null ? getString(R.string.dialog_title_edit_budget) : category.label)
        .neg();
    if (category != null) {
      long allocated = parentItem == null ? getAllocated() :
          Stream.of(parentItem.getChildren()).mapToLong(category1 -> category1.budget).sum();
      final Long budget = parentItem == null ? this.budget.getAmount().getAmountMinor() : parentItem.budget;
      long allocatable = budget - allocated;
      final long maxLong = allocatable + category.budget;
      if (maxLong <= 0) {
        ((ProtectedFragmentActivity) getActivity()).showSnackbar(TextUtils.concatResStrings(getActivity(), " ",
            parentItem == null? R.string.budget_exceeded_error_1_2 : R.string.sub_budget_exceeded_error_1_2,
            parentItem == null? R.string.budget_exceeded_error_2 : R.string.sub_budget_exceeded_error_2),
            Snackbar.LENGTH_LONG);
        return;
      }
      Bundle bundle = new Bundle(1);
      bundle.putLong(KEY_CATID, category.id);
      simpleFormDialog.extra(bundle);
      amount = new Money(currencyUnit, category.budget);
      max = new Money(currencyUnit, maxLong);
      min = parentItem != null ? null : new Money(currencyUnit, Stream.of(category.getChildren()).mapToLong(category1 -> category1.budget).sum());
    } else {
      amount = this.budget.getAmount();
      max = null;
      min = new Money(currencyUnit, getAllocated());
    }
    simpleFormDialog
        .fields(buildAmountField(amount.getAmountMajor(), max == null ? null : max.getAmountMajor(),
            min == null ? null : min.getAmountMajor(), category != null, parentItem != null))
        .show(this, EDIT_BUDGET_DIALOG);
  }

  private AmountEdit buildAmountField(BigDecimal amount, BigDecimal max, BigDecimal min, boolean isMainCategory, boolean isSubCategory) {
    final AmountEdit amountEdit = AmountEdit.plain(KEY_AMOUNT)
        .label(appendCurrencySymbol(getContext(), R.string.budget_allocated_amount, currencyUnit))
        .fractionDigits(currencyUnit.fractionDigits()).required();
    if (amount != null && !(amount.compareTo(BigDecimal.ZERO) == 0)) {
      amountEdit.amount(amount);
    }
    if (max != null) {
      amountEdit.max(max, String.format(Locale.ROOT, "%s %s",
          getString(isSubCategory ? R.string.sub_budget_exceeded_error_1_1: R.string.budget_exceeded_error_1_1, max),
          getString(isSubCategory ? R.string.sub_budget_exceeded_error_2: R.string.budget_exceeded_error_2)));
    }
    if (min != null) {
      amountEdit.min(min, getString(isMainCategory ? R.string.sub_budget_under_allocated_error : R.string.budget_under_allocated_error, min));
    }
    return amountEdit;
  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    if (which == BUTTON_POSITIVE) {
      final Money amount = new Money(currencyUnit, (BigDecimal) extras.getSerializable(KEY_AMOUNT));
      if (dialogTag.equals(EDIT_BUDGET_DIALOG)) {
        viewModel.updateBudget(this.budget.getId(), extras.getLong(KEY_CATID), amount);
      }
      return true;
    }
    return false;
  }

  public void loadBudget(long budgetId) {
    viewModel.loadBudget(budgetId, false);
  }

  private void setBudget(@NonNull Budget budget) {
    this.budget = budget;
    currencyUnit = budget.getCurrency().equals(AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE)
        ? Utils.getHomeCurrency() : currencyContext.get(budget.getCurrency());
    setAccountInfo(new AccountInfo() {
      @Override
      public long getId() {
        return budget.getAccountId();
      }

      @Override
      public CurrencyUnit getCurrencyUnit() {
        return currencyUnit;
      }
    });
    final ActionBar actionBar = ((ProtectedFragmentActivity) getActivity()).getSupportActionBar();
    actionBar.setTitle(budget.getTitle());
    budgetProgress.setFinishedStrokeColor(budget.getColor());
    budgetProgress.setUnfinishedStrokeColor(getContrastColor(budget.getColor()));
    if (mAdapter == null) {
      mAdapter = new BudgetAdapter((ProtectedFragmentActivity) getActivity(), currencyFormatter,
          currencyContext.get(budget.getCurrency()), this);
      mListView.setAdapter(mAdapter);
    }
    mGrouping = budget.getGrouping();
    mGroupingYear = 0;
    mGroupingSecond = 0;
    updateDateInfo(false);
    updateTotals();
  }

  @Override
  public void onBudgetClick(Category category, Category parentItem) {
    showEditBudgetDialog(category, parentItem);
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
    totalAllocated.setText(currencyFormatter.formatCurrency(new Money(currencyUnit,
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
    totalAmount.setText(currencyFormatter.formatCurrency(new Money(currencyUnit, -spent)));
    final Long allocated = this.budget.getAmount().getAmountMinor();
    long available = allocated - spent;
    totalAvailable.setText(currencyFormatter.formatCurrency(new Money(currencyUnit, available)));
    boolean onBudget = available >=0;
    totalAvailable.setBackgroundResource(getBackgroundForAvailable(onBudget, context.getThemeType()));
    totalAvailable.setTextColor(onBudget ? context.getColorIncome() :
        context.getColorExpense());
    int progress = allocated == 0 ? 100 : Math.round(spent * 100F / allocated);
    UiUtils.configureProgress(budgetProgress, progress);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.budget, menu);
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
