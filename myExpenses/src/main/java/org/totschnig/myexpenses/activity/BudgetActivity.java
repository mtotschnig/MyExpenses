package org.totschnig.myexpenses.activity;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.Menu;
import android.view.MenuItem;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.adapter.BudgetAdapter;
import org.totschnig.myexpenses.fragment.BudgetFragment;
import org.totschnig.myexpenses.model.AggregateAccount;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.TextUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.viewmodel.BudgetViewModel;
import org.totschnig.myexpenses.viewmodel.data.Budget;
import org.totschnig.myexpenses.viewmodel.data.Category;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import eltos.simpledialogfragment.form.AmountEdit;
import eltos.simpledialogfragment.form.FormElement;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import eltos.simpledialogfragment.form.SimpleFormDialogWithoutDefaultFocus;
import eltos.simpledialogfragment.form.Spinner;
import eltos.simpledialogfragment.input.SimpleInputDialog;

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
  private String currency;
  private List<Budget> budgetList;
  private Budget currentBudget;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    super.onCreate(savedInstanceState);
    budgetViewModel = ViewModelProviders.of(this).get(BudgetViewModel.class);
    accountId = getIntent().getLongExtra(KEY_ACCOUNTID, 0);
    currency = getIntent().getStringExtra(KEY_CURRENCY);
    if (currency == null) {
      throw new NullPointerException();
    }
    budgetViewModel.getData().observe(this, result -> {
      if (result.isEmpty()) {
        showNewBudgetDialog(null);
      } else {
        Grouping currentType = getCurrentTypeFromPreference();
        budgetList = result;
        setBudget(Stream.of(budgetList).filter(
            budget -> budget.getGrouping().equals(currentType)).findFirst().orElse(budgetList.get(0)));
        invalidateOptionsMenu();
      }
    });
    budgetViewModel.loadBudgets(accountId, currency,
        cursor -> {
          final boolean isHomeAggregate = isHomeAggregate();
          Currency currency = getCurrency();
          return new Budget(cursor.getLong(0), accountId, currency,
              Grouping.valueOf(cursor.getString(1)),
              new Money(currency, cursor.getLong(2)), isHomeAggregate);
        });
  }

  private void setBudget(Budget budget) {
    currentBudget = budget;
    mListFragment.setBudget(budget);
  }

  private void showNewBudgetDialog(Grouping newType) {
    final AmountEdit amountEdit = buildAmountField(null, null, null, false, false);
    final FormElement[] fields;
    if (newType == null) {
      final Spinner typeSpinner = Spinner.plain(KEY_TYPE).label(R.string.type)
        .items(Stream.of(Budget.BUDGET_TYPES)
            .map(grouping -> grouping.getBudgetLabel(this)).toArray(String[]::new))
        .required().preset(0);
      fields = new FormElement[]{typeSpinner, amountEdit};
    } else {
      fields = new FormElement[]{amountEdit};
    }
    final SimpleFormDialog simpleFormDialog = new SimpleFormDialogWithoutDefaultFocus()
        .title(R.string.dialog_title_new_budget)
        .neg()
        .fields(fields);
    if (newType != null) {
      Bundle extras = new Bundle(1);
      extras.putSerializable(KEY_BUDGET_TYPE, newType);
      simpleFormDialog.extra(extras);
    }
    simpleFormDialog.show(this, NEW_BUDGET_DIALOG);
  }

  private AmountEdit buildAmountField(BigDecimal amount, BigDecimal max, BigDecimal min, boolean isMainCategory, boolean isSubCategory) {
    final Currency currency = getCurrency();
    final AmountEdit amountEdit = AmountEdit.plain(KEY_AMOUNT)
        .label(appendCurrencySymbol(this, R.string.budget_allocated_amount, currency))
        .fractionDigits(Money.getFractionDigits(currency)).required();
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
    final SimpleFormDialog simpleFormDialog = new SimpleFormDialogWithoutDefaultFocus()
        .title(category == null ? getString(R.string.dialog_title_edit_budget) : category.label)
        .neg();
    final Currency currency = getCurrency();
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
      amount = new Money(currency, category.budget);
      max = new Money(currency, maxLong);
      min = parentItem != null ? null : new Money(currency, Stream.of(category.getChildren()).mapToLong(category1 -> category1.budget).sum());
    } else {
      amount = currentBudget.getAmount();
      max = null;
      min = new Money(currency, mListFragment.getAllocated());
    }
    simpleFormDialog
        .fields(buildAmountField(amount.getAmountMajor(), max == null ? null : max.getAmountMajor(),
            min == null ? null : min.getAmountMajor(), category != null, parentItem != null))
        .show(this, EDIT_BUDGET_DIALOG);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.budget, menu);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    Utils.configureGroupingMenu(menu.findItem(R.id.GROUPING_COMMAND).getSubMenu(), getCurrentTypeFromPreference());
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
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
    prefHandler.putString(getPrefKey(), newType.name());
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
    final String typeFromPreference = prefHandler.getString(getPrefKey(), null);
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
    return PREFKEY_PREFIX + (accountId != 0 ? String.valueOf(accountId) : currency);
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
      Currency currency = getCurrency();
      final Money amount = new Money(currency, (BigDecimal) extras.getSerializable(KEY_AMOUNT));
      if (dialogTag.equals(NEW_BUDGET_DIALOG)) {
        final boolean isHomeAggregate = isHomeAggregate();
        Grouping budgetType = extras.containsKey(KEY_BUDGET_TYPE) ?
            (Grouping) extras.getSerializable(KEY_BUDGET_TYPE) :
            Budget.BUDGET_TYPES[extras.getInt(KEY_TYPE)];
        Budget budget = new Budget(0, accountId, currency, budgetType,
            amount, isHomeAggregate);
        persistTypeToPreference(budgetType);
        budgetViewModel.createBudget(budget);
        return true;
      } else if (dialogTag.equals(EDIT_BUDGET_DIALOG)) {
        budgetViewModel.updateBudget(currentBudget.getId(), extras.getLong(KEY_CATID), amount);
      }
    }
    return false;
  }

  protected Currency getCurrency() {
    return Currency.getInstance(isHomeAggregate() ?
        prefHandler.getString(PrefKey.HOME_CURRENCY, "EUR") : this.currency);
  }

  protected boolean isHomeAggregate() {
    return AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE.equals(this.currency);
  }

  @Override
  public void onBudgetClick(Category category, Category parentItem) {
    showEditBudgetDialog(category, parentItem);
  }
}
