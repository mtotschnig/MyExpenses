package org.totschnig.myexpenses.activity;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.MenuItem;
import android.view.View;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.adapter.BudgetAdapter;
import org.totschnig.myexpenses.fragment.BudgetFragment;
import org.totschnig.myexpenses.model.AggregateAccount;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.util.TextUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.viewmodel.BudgetViewModel;
import org.totschnig.myexpenses.viewmodel.data.Budget;
import org.totschnig.myexpenses.viewmodel.data.Category;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import eltos.simpledialogfragment.form.AmountEdit;
import eltos.simpledialogfragment.form.FormElement;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import eltos.simpledialogfragment.form.Spinner;
import eltos.simpledialogfragment.input.SimpleInputDialog;

import static org.totschnig.myexpenses.activity.ManageCategories.ACTION_MANAGE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;
import static org.totschnig.myexpenses.util.TextUtils.appendCurrencySymbol;

public class BudgetActivity extends CategoryActivity<BudgetFragment> implements
    SimpleInputDialog.OnDialogResultListener, BudgetAdapter.OnBudgetClickListener {

  public static final String ACTION_BUDGET = "ACTION_BUDGET";
  private static final String NEW_BUDGET_DIALOG = "NEW_BUDGET";
  private static final String EDIT_BUDGET_DIALOG = "EDIT_BUDGET";
  private static final String KEY_BUDGET_TYPE = "budgetType";
  private static final String PREFKEY_PREFIX = "current_budgetType_";
  private BudgetViewModel budgetViewModel;
  private long accountId;
  @NonNull
  private CurrencyUnit currencyUnit;
  private boolean isHomeAggregate;
  private List<Budget> budgetList;
  private Budget currentBudget;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(getThemeId());
    super.onCreate(savedInstanceState);
    budgetViewModel = ViewModelProviders.of(this).get(BudgetViewModel.class);
    accountId = getIntent().getLongExtra(KEY_ACCOUNTID, 0);
    String currency = getIntent().getStringExtra(KEY_CURRENCY);
    if (currency == null) {
      throw new NullPointerException();
    }
    isHomeAggregate = AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE.equals(currency);
    currencyUnit = isHomeAggregate ? Utils.getHomeCurrency() : currencyContext.get(currency);
    budgetViewModel.getData().observe(this, result -> {
      if (result.isEmpty()) {
        if (getSupportFragmentManager().findFragmentByTag(NEW_BUDGET_DIALOG) == null) {
          showNewBudgetDialog(null);
        }
        findViewById(R.id.fragment_container).setVisibility(View.INVISIBLE);
      } else {
        findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
        Grouping currentType = getCurrentTypeFromPreference();
        budgetList = result;
        invalidateOptionsMenu();
        setBudget(Stream.of(budgetList).filter(
            budget -> budget.getGrouping().equals(currentType)).findFirst().orElse(budgetList.get(0)));
        invalidateOptionsMenu();
      }
    });
    budgetViewModel.loadBudgets(accountId, currency,
        cursor -> new Budget(cursor.getLong(0), accountId, currencyUnit,
            Grouping.valueOf(cursor.getString(1)),
            new Money(currencyUnit, cursor.getLong(2)), isHomeAggregate));
  }

  private void setBudget(Budget budget) {
    currentBudget = budget;
    mListFragment.setBudget(budget);
  }

  private void showNewBudgetDialog(Grouping newType) {
    final AmountEdit amountEdit = buildAmountField(null, null, null, false, false);
    final FormElement[] fields;
    final int dialog_title_new_budget;
    boolean autofocus;
    if (newType == null) {
      final Spinner typeSpinner = Spinner.plain(KEY_TYPE).label(R.string.type)
        .items(Stream.of(Budget.BUDGET_TYPES)
            .map(this::getBudgetLabelForSpinner)
            .map(this::getString)
            .toArray(String[]::new))
        .required().preset(0);
      fields = new FormElement[]{typeSpinner, amountEdit};
      dialog_title_new_budget = R.string.dialog_title_new_budget;
      autofocus = false;
    } else {
      dialog_title_new_budget = getBudgetLabelForDialogTitle(newType);
      fields = new FormElement[]{amountEdit};
      autofocus = true;
    }
    final SimpleFormDialog simpleFormDialog = new SimpleFormDialog()
        .title(dialog_title_new_budget)
        .neg()
        .fields(fields)
        .autofocus(autofocus);
    if (newType != null) {
      Bundle extras = new Bundle(1);
      extras.putSerializable(KEY_BUDGET_TYPE, newType);
      simpleFormDialog.extra(extras);
    }
    simpleFormDialog.show(this, NEW_BUDGET_DIALOG);
  }

  private AmountEdit buildAmountField(BigDecimal amount, BigDecimal max, BigDecimal min, boolean isMainCategory, boolean isSubCategory) {
    final AmountEdit amountEdit = AmountEdit.plain(KEY_AMOUNT)
        .label(appendCurrencySymbol(this, R.string.budget_allocated_amount, currencyUnit))
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

  private void showEditBudgetDialog(Category category, Category parentItem) {
    final Money amount, max, min;
    final SimpleFormDialog simpleFormDialog = new SimpleFormDialog()
        .title(category == null ? getString(R.string.dialog_title_edit_budget) : category.label)
        .neg();
    if (category != null) {
      long allocated = parentItem == null ? mListFragment.getAllocated() :
          Stream.of(parentItem.getChildren()).mapToLong(category1 -> category1.budget).sum();
      final Long budget = parentItem == null ? currentBudget.getAmount().getAmountMinor() : parentItem.budget;
      long allocatable = budget - allocated;
      final long maxLong = allocatable + category.budget;
      if (maxLong <= 0) {
        showSnackbar(TextUtils.concatResStrings(this, " ",
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
      amount = currentBudget.getAmount();
      max = null;
      min = new Money(currencyUnit, mListFragment.getAllocated());
    }
    simpleFormDialog
        .fields(buildAmountField(amount.getAmountMajor(), max == null ? null : max.getAmountMajor(),
            min == null ? null : min.getAmountMajor(), category != null, parentItem != null))
        .show(this, EDIT_BUDGET_DIALOG);
  }

  public boolean hasBudgets() {
    return budgetList != null;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.MANAGE_CATEGORIES_COMMAND) {
      Intent i = new Intent(this, ManageCategories.class);
      i.setAction(ACTION_MANAGE);
      startActivity(i);
      return true;
    }
    return handleGrouping(item) || super.onOptionsItemSelected(item);
  }

  private boolean handleGrouping(MenuItem item) {
    Grouping newGrouping = Utils.getGroupingFromMenuItemId(item.getItemId());
    if (newGrouping != null) {
      if (!item.isChecked()) {
        switchBudget(newGrouping);
        invalidateOptionsMenu();
      }
      return true;
    }
    return false;
  }

  private void persistTypeToPreference(Grouping newType) {
    getPrefHandler().putString(getPrefKey(), newType.name());
  }

  private void switchBudget(Grouping newGrouping) {
    Optional<Budget> newBudget = Stream.of(budgetList).filter(budget -> budget.getGrouping() == newGrouping).findSingle();
    if (newBudget.isPresent()) {
      persistTypeToPreference(newGrouping);
      setBudget(newBudget.get());
    } else {
      showNewBudgetDialog(newGrouping);
    }
  }

  private @NonNull Grouping getCurrentTypeFromPreference() {
    final String typeFromPreference = getPrefHandler().getString(getPrefKey(), null);
    if (typeFromPreference != null) {
      try {
        return Grouping.valueOf(typeFromPreference);
      } catch (IllegalArgumentException ignored) {
      }
    }
    return Grouping.MONTH;
  }

  @NonNull
  private String getPrefKey() {
    return PREFKEY_PREFIX + (accountId != 0 ? String.valueOf(accountId) :
        (isHomeAggregate ? AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE : currencyUnit.code()));
  }

  @NonNull
  @Override
  public String getAction() {
    return ACTION_BUDGET;
  }

  @Override
  protected int getContentView() {
    return R.layout.activity_budget;
  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    if (super.onResult(dialogTag, which, extras)) {
      return true;
    }
    if (which == BUTTON_POSITIVE) {
      final Money amount = new Money(currencyUnit, (BigDecimal) extras.getSerializable(KEY_AMOUNT));
      if (dialogTag.equals(NEW_BUDGET_DIALOG)) {
        Grouping budgetType = extras.containsKey(KEY_BUDGET_TYPE) ?
            (Grouping) extras.getSerializable(KEY_BUDGET_TYPE) :
            Budget.BUDGET_TYPES[extras.getInt(KEY_TYPE)];
        Budget budget = new Budget(0, accountId, currencyUnit, budgetType,
            amount, isHomeAggregate);
        persistTypeToPreference(budgetType);
        budgetViewModel.createBudget(budget);
      } else if (dialogTag.equals(EDIT_BUDGET_DIALOG)) {
        budgetViewModel.updateBudget(currentBudget.getId(), extras.getLong(KEY_CATID), amount);
      }
      return true;
    } else if (!hasBudgets()) {
      finish();
    }
    return false;
  }

  @Override
  public void onBudgetClick(Category category, Category parentItem) {
    showEditBudgetDialog(category, parentItem);
  }

  public int getBudgetLabelForDialogTitle(Grouping type) {
    switch (type) {
      case DAY:
        return R.string.daily_budget;
      case WEEK:
        return R.string.weekly_budget;
      case MONTH:
        return R.string.monthly_budget;
      case YEAR:
        return R.string.yearly_budget;
    }
    throw new IllegalStateException();
  }

  public int getBudgetLabelForSpinner(Grouping type) {
    switch (type) {
      case DAY:
        return R.string.daily_plain;
      case WEEK:
        return R.string.weekly_plain;
      case MONTH:
        return R.string.monthly;
      case YEAR:
        return R.string.yearly_plain;
    }
    throw new IllegalStateException();
  }

  public static int getBackgroundForAvailable(boolean onBudget, ProtectedFragmentActivity.ThemeType themeType) {
    boolean darkTheme = themeType == ProtectedFragmentActivity.ThemeType.dark;
    return onBudget ?
        (darkTheme ? R.drawable.round_background_income_dark : R.drawable.round_background_income_light) :
        (darkTheme ? R.drawable.round_background_expense_dark : R.drawable.round_background_expense_light);
  }
}
