package org.totschnig.myexpenses.activity;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.fragment.BudgetFragment;
import org.totschnig.myexpenses.model.AggregateAccount;
import org.totschnig.myexpenses.model.BudgetType;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.viewmodel.BudgetViewModel;
import org.totschnig.myexpenses.viewmodel.data.Budget;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import eltos.simpledialogfragment.form.AmountEdit;
import eltos.simpledialogfragment.form.FormElement;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import eltos.simpledialogfragment.form.SimpleFormDialogWithoutDefaultFocus;
import eltos.simpledialogfragment.form.Spinner;
import eltos.simpledialogfragment.input.SimpleInputDialog;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;
import static org.totschnig.myexpenses.util.TextUtils.appendCurrencySymbol;

public class BudgetActivity extends CategoryActivity<BudgetFragment> implements
    SimpleInputDialog.OnDialogResultListener {

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
        BudgetType currentType = getCurrentTypeFromPreference();
        budgetList = result;
        setBudget(Stream.of(budgetList).filter(
            budget -> budget.getType().equals(currentType)).findFirst().orElse(budgetList.get(0)));
        invalidateOptionsMenu();
      }
    });
    budgetViewModel.loadBudgets(accountId, currency,
        cursor -> {
          final boolean isHomeAggregate = isHomeAggregate();
          Currency currency = getCurrency();
          return new Budget(cursor.getLong(0), accountId, currency,
              BudgetType.valueOf(cursor.getString(1)),
              new Money(currency, cursor.getLong(2)), isHomeAggregate);
        });
  }

  private void setBudget(Budget budget) {
    currentBudget = budget;
    mListFragment.setBudget(budget);
  }

  private void showNewBudgetDialog(BudgetType newType) {
    final Spinner typeSpinner = Spinner.plain(KEY_TYPE).label(R.string.type)
        .items(Stream.of(BudgetType.values())
            .map(type -> type.getLabel(this)).toArray(String[]::new))
        .required().preset(0);
    final AmountEdit amountEdit = buildAmountField(null);
    final FormElement[] fields = newType == null ? new FormElement[]{typeSpinner, amountEdit} :
        new FormElement[]{amountEdit};
    final SimpleFormDialog simpleFormDialog = new SimpleFormDialogWithoutDefaultFocus()
        .title(R.string.dialog_title_new_budget)
        .fields(fields);
    if (newType != null) {
      Bundle extras = new Bundle(1);
      extras.putSerializable(KEY_BUDGET_TYPE, newType);
      simpleFormDialog.extra(extras);
    }
    simpleFormDialog.show(this, NEW_BUDGET_DIALOG);
  }

  private AmountEdit buildAmountField(BigDecimal amount) {
    final Currency currency = getCurrency();
    final AmountEdit amountEdit = AmountEdit.plain(KEY_AMOUNT)
        .label(appendCurrencySymbol(this, R.string.budget_allocated_amount, currency))
        .fractionDigits(Money.getFractionDigits(currency)).required();
    if (amount != null) {
      amountEdit.amount(amount);
    }
    return amountEdit;
  }

  private void showEditBudgetDialog() {
    new SimpleFormDialogWithoutDefaultFocus()
        .title(R.string.dialog_title_edit_budget)
        .fields(buildAmountField(currentBudget.getAmount().getAmountMajor()))
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
    Utils.configureGroupingMenu(menu.findItem(R.id.GROUPING_COMMAND).getSubMenu(), getCurrentTypeFromPreference().toGrouping());
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (handleGrouping(item)) return true;
    switch (item.getItemId()) {
      case R.id.EDIT_COMMAND:
        showEditBudgetDialog();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private boolean handleGrouping(MenuItem item) {
    Grouping newGrouping = Utils.getGroupingFromMenuItemId(item.getItemId());
    if (newGrouping != null) {
      if (!item.isChecked()) {
        switchBudget(BudgetType.fromGrouping(newGrouping));
        invalidateOptionsMenu();
      }
      return true;
    }
    return false;
  }

  private void persistTypeToPreference(BudgetType newType) {
    prefHandler.putString(getPrefKey(), newType.name());
  }

  private void switchBudget(BudgetType newType) {
    Optional<Budget> newBudget = Stream.of(budgetList).filter(budget -> budget.getType() == newType).findSingle();
    if (newBudget.isPresent()) {
      persistTypeToPreference(newType);
      setBudget(newBudget.get());
    } else {
      showNewBudgetDialog(newType);
    }
  }

  private @NonNull BudgetType getCurrentTypeFromPreference() {
    final String typeFromPreference = prefHandler.getString(getPrefKey(), null);
    if (typeFromPreference != null) {
      try {
        return BudgetType.valueOf(typeFromPreference);
      } catch (IllegalArgumentException ignored) {
      }
    }
    return BudgetType.MONTHLY;
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
        BudgetType budgetType = extras.containsKey(KEY_BUDGET_TYPE) ?
            (BudgetType) extras.getSerializable(KEY_BUDGET_TYPE) :
            BudgetType.values()[extras.getInt(KEY_TYPE)];
        Budget budget = new Budget(0, accountId, currency, budgetType,
            amount, isHomeAggregate);
        persistTypeToPreference(budgetType);
        budgetViewModel.createBudget(budget);
        return true;
      } else if (dialogTag.equals(EDIT_BUDGET_DIALOG)) {
        budgetViewModel.updateBudget(currentBudget.getId(), amount);
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
}
